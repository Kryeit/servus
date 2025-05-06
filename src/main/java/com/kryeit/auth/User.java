package com.kryeit.auth;

import com.google.gson.JsonObject;
import com.kryeit.Database;
import io.javalin.http.Context;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class User {
    private final UUID uuid;
    private Data data;

    private User(UUID uuid, Data data) {
        this.uuid = uuid;
        this.data = data;
    }

    public Data data() {
        if (data == null) {
            data = getData();
        }
        return data;
    }

    public UUID uuid() {
        return uuid;
    }

    public static Optional<User> fromRequest(Context ctx) {
        String cookie = ctx.cookie("auth");
        if (cookie == null) {
            return Optional.empty();
        }

        // Read the token and process it
        UUID userId = Jwt.validateToken(cookie);

        return Optional.of(new User(userId, null));
    }

    private Data getData() {
        return Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT username, creation, last_seen, roles, stats
                        FROM users
                        WHERE uuid = cast(:uuid as uuid)
                        """)
                .bind("uuid", uuid)
                .mapTo(Data.class)
                .first());
    }

    public record Data(String username, Timestamp creation, Timestamp lastSeen, List<Role> roles, JsonObject stats) {
    }

    public enum Role {
        DEFAULT,
        KRYEITOR,
        COLLABORATOR,
        STAFF
        ;
    }
}