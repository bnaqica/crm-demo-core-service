package com.bnaqica.crmdemocoreservice.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * A portal submission routed to an internal team. {@code id} is the portal's submission id; {@code version} is the
 * portal's version of it (corrections raise it). {@code rowVersion} guards against two people deciding at once.
 */
@Entity
@Table(name = "submission")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
@AllArgsConstructor
public class Submission {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portal_user_id", nullable = false)
    private PortalUser portalUser;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String region;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    /** Shown to the submitter; set only when REJECTED. */
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** The {@code sub} of the internal user who accepted or rejected it. */
    @Column(name = "decided_by")
    private String decidedBy;

    @ElementCollection
    @BatchSize(size = 50)
    @CollectionTable(name = "submission_document", joinColumns = @JoinColumn(name = "submission_id"))
    @OrderColumn(name = "position")
    @Builder.Default
    private List<SubmissionDocument> documents = new ArrayList<>();

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;
}
