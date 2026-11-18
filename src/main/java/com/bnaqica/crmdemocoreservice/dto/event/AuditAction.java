package com.bnaqica.crmdemocoreservice.dto.event;

/** What core-service audits. */
public enum AuditAction {
    /** A request was refused: missing permission, inactive user, or a team the user can't change or manage. */
    AUTHORIZATION_DENIED,
    /** A Super Manager read a record of a team they don't belong to. */
    CROSS_TEAM_READ
}
