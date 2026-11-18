package com.bnaqica.crmdemocoreservice.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * What the current user may see and do, for the CRM UI to hide what they can't. Not an authorization: every API call
 * is still checked.
 *
 * @param permissions effective permissions, {@code :write} implying {@code :read}, sorted
 * @param uiFeatures the user's {@code ui:*} features without the prefix, sorted
 */
@Builder
public record CapabilitiesResponse(
        String sub,
        String name,
        List<String> permissions,
        List<String> uiFeatures,
        List<UUID> memberTeams,
        List<UUID> managedTeams,
        boolean allTeams) {}
