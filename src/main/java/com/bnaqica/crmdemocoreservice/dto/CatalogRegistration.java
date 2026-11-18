package com.bnaqica.crmdemocoreservice.dto;

import java.util.List;

/** Body of {@code PUT /internal/catalog/{service}}: every permission the service enforces. */
public record CatalogRegistration(List<Permission> permissions) {

    public record Permission(String key, String realm, String description, boolean sensitive) {}
}
