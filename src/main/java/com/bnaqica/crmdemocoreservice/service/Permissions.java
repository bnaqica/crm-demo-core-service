package com.bnaqica.crmdemocoreservice.service;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/** The permission rules shared by enforcement and the capabilities endpoint. */
public final class Permissions {

    private static final String READ = ":read";
    private static final String WRITE = ":write";

    private Permissions() {}

    /** {@code granted} plus the {@code <resource>:read} implied by each {@code <resource>:write}. */
    public static Set<String> effective(Collection<String> granted) {
        var effective = new TreeSet<String>(granted);
        for (String permission : granted) {
            if (permission.endsWith(WRITE)) {
                effective.add(permission.substring(0, permission.length() - WRITE.length()) + READ);
            }
        }
        return effective;
    }

    public static boolean isResourcePermission(String key) {
        return !key.startsWith("ui:") && (key.endsWith(READ) || key.endsWith(WRITE));
    }

    public static boolean isWrite(String key) {
        return key.endsWith(WRITE);
    }
}
