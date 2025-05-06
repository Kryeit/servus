package com.kryeit.panel.auth;

import com.kryeit.Database;
import com.kryeit.auth.Jwt;
import com.kryeit.auth.User;
import io.javalin.http.Context;

import java.util.Optional;
import java.util.UUID;

public record Admin(long id, String username, String password) {

    public static Optional<Admin> fromRequest(Context ctx) {
        String cookie = ctx.cookie("auth");
        if (cookie == null) {
            return Optional.empty();
        }

        // Read the token and process it
        long userId = AdminJwt.validateToken(cookie);

        Admin admin = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT *
                        FROM admins
                        WHERE id = :id
                        """)
                .bind("id", userId)
                .mapTo(Admin.class)
                .first());

        return Optional.of(admin);
    }
}