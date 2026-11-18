package com.bnaqica.crmdemocoreservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/** core-service's copy of a portal user, kept current from {@code portal-intake} events. {@code id} is the portal {@code sub}. */
@Entity
@Table(name = "portal_user")
@BatchSize(size = 50) // a page of submissions loads its submitters together
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
@AllArgsConstructor
public class PortalUser {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
