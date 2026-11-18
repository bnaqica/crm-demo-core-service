package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.client.UmsClient;
import com.bnaqica.crmdemocoreservice.config.CrmProperties;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.exception.UmsClientException;
import com.bnaqica.crmdemocoreservice.exception.UmsUnavailableException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class TeamScopeServiceImpl implements TeamScopeService {

    private final UmsClient ums;
    private final JsonMapper jsonMapper;
    private final Cache<String, TeamScope> cache;

    @Autowired
    public TeamScopeServiceImpl(UmsClient ums, JsonMapper jsonMapper, Clock clock, CrmProperties properties) {
        this(ums, jsonMapper, clock, properties.scopeCache().ttl());
    }

    TeamScopeServiceImpl(UmsClient ums, JsonMapper jsonMapper, Clock clock, Duration ttl) {
        this.ums = ums;
        this.jsonMapper = jsonMapper;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .ticker(() -> TimeUnit.MILLISECONDS.toNanos(clock.millis()))
                .maximumSize(10_000)
                .build();
    }

    @Override
    public TeamScope cached(String sub) {
        TeamScope cached = cache.getIfPresent(sub);
        return cached != null ? cached : live(sub);
    }

    /**
     * Fetches inside the cache's per-key compute, so an eviction that arrives during the fetch waits for it and then
     * removes the (possibly stale) result. Only ACTIVE scopes are cached: there is no event when a user becomes ACTIVE,
     * so a cached refusal could otherwise outlive it by the whole TTL.
     */
    @Override
    public TeamScope live(String sub) {
        var fetched = new TeamScope[1];
        cache.asMap().compute(sub, (key, previous) -> {
            fetched[0] = fetch(key);
            return fetched[0].isActive() ? fetched[0] : null;
        });
        return fetched[0];
    }

    private TeamScope fetch(String sub) {
        try {
            return ums.teamScope(sub);
        } catch (UmsClientException ex) {
            if (ex.isNotFound()) {
                return TeamScope.unknown(sub);
            }
            throw new UmsUnavailableException("The UMS couldn't return the team scope of " + sub, ex);
        } catch (RestClientException | OAuth2AuthorizationException ex) {
            throw new UmsUnavailableException("The UMS couldn't be reached for the team scope of " + sub, ex);
        }
    }

    @Override
    public void onUserEvent(String payload) {
        JsonNode event = jsonMapper.readTree(payload);
        JsonNode sub = event.get("sub");
        if (sub == null || sub.isNull()) {
            log.debug("Ignoring user event without a sub");
            return;
        }
        cache.invalidate(sub.asString());
        log.info(
                "Evicted cached team scope of {} after {}",
                sub.asString(),
                event.path("type").asString());
    }
}
