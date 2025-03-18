package com.kryeit.merch;

import java.sql.Timestamp;

public record Product(long id, String name, String description, long price,
                      String size, String color, String material, boolean virtual,
                      boolean listed, Timestamp createdAt, Timestamp updatedAt) {
}