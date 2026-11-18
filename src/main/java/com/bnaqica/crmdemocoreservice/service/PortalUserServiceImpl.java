package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.entity.PortalUser;
import com.bnaqica.crmdemocoreservice.repository.PortalUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PortalUserServiceImpl implements PortalUserService {

    private final PortalUserRepository repository;
    private final Clock clock;

    @Override
    public PortalUser upsert(UUID id, String email) {
        PortalUser user = repository
                .findById(id)
                .orElseGet(() -> PortalUser.builder().id(id).build());
        if (!email.equals(user.getEmail())) {
            user.setEmail(email);
            user.setUpdatedAt(Instant.now(clock));
            log.info("Recorded email of portal user {}", id);
        }
        return repository.save(user);
    }
}
