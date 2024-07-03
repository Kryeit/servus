package com.kryeit.servus.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Permission {

    ADMIN_READ("staff:read"),
    ADMIN_UPDATE("staff:update"),
    ADMIN_CREATE("staff:create"),
    ADMIN_DELETE("staff:delete"),

    ;

    @Getter
    private final String permission;
}
