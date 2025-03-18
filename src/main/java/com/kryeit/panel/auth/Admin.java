package com.kryeit.panel.auth;

import com.kryeit.Database;

public class Admin {
    private final long id;
    private Data data;

    private Admin(long id, Data data) {
        this.id = id;
        this.data = data;
    }

    public Data data() {
        if (data == null) {
            data = getData();
        }
        return data;
    }

    public long id() {
        return id;
    }


    private Data getData() {
        return Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT username, password
                        FROM admins
                        WHERE id = :id
                        """)
                .bind("id", id)
                .mapTo(Data.class)
                .first());
    }

    public record Data(String username, String password) {
    }
}