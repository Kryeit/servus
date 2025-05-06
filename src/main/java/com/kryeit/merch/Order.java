package com.kryeit.merch;

import com.google.gson.JsonObject;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public record Order(
        long id, UUID uuid, Long[] cart, String destination, String phone,
        String email, Status status, String transaction, Timestamp creation, Timestamp edition) {
    public enum Status {
        UNPAID,
        PENDING,
        SHIPPED,
        DELIVERED
    }
}