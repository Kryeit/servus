package com.kryeit.merch;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public record Order(long id, UUID uuid, List<Long> cart, String destination, String phone, String email, Status status, String transaction, Timestamp createdAt) {
    public enum Status {
        UNPAID,
        PENDING,
        SHIPPED,
        DELIVERED
    }
}