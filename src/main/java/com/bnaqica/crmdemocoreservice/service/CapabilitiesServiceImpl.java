package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.dto.response.CapabilitiesResponse;
import com.bnaqica.crmdemocoreservice.exception.InactiveUserException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/** Built from the token's permissions and the live team scope, which also re-checks that the user is ACTIVE. */
@Service
@RequiredArgsConstructor
public class CapabilitiesServiceImpl implements CapabilitiesService {

    private static final String UI_PREFIX = "ui:";

    private final TeamScopeService teamScope;
    private final AuthorizationAuditService audit;

    @Override
    public CapabilitiesResponse current() {
        Jwt token = CurrentUser.token().orElseThrow(() -> new InactiveUserException("anonymous", "not signed in"));
        TeamScope scope = teamScope.live(token.getSubject());
        if (!scope.isActive()) {
            audit.denied(null, "user status " + scope.status());
            throw new InactiveUserException(token.getSubject(), scope.status());
        }
        List<String> granted = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return CapabilitiesResponse.builder()
                .sub(token.getSubject())
                .name(
                        token.hasClaim("name")
                                ? token.getClaimAsString("name")
                                : token.getClaimAsString("preferred_username"))
                .permissions(Permissions.effective(granted).stream()
                        .filter(Permissions::isResourcePermission)
                        .toList())
                .uiFeatures(granted.stream()
                        .filter(key -> key.startsWith(UI_PREFIX))
                        .map(key -> key.substring(UI_PREFIX.length()))
                        .sorted()
                        .toList())
                .memberTeams(sorted(scope.memberTeams()))
                .managedTeams(sorted(scope.managedTeams()))
                .allTeams(scope.allTeams())
                .build();
    }

    private static List<UUID> sorted(Collection<UUID> teams) {
        return teams.stream().sorted().toList();
    }
}
