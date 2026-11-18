package com.bnaqica.crmdemocoreservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sends submissions of {@code type} from {@code region} to {@code teamId}. Rules are tried by ascending {@code position}. */
@Entity
@Table(name = "routing_rule")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
@AllArgsConstructor
public class RoutingRule {

    @Id
    private UUID id;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String region;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;
}
