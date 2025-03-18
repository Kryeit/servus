package com.kryeit;

import org.jdbi.v3.core.Jdbi;

public class DatabaseUtils {
    public static void createTables() {
        Jdbi jdbi = Database.getJdbi();

        jdbi.useHandle(handle -> {
            // Create admins table
            handle.execute("""
                CREATE TABLE IF NOT EXISTS admins (
                    id BIGSERIAL PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    password VARCHAR(255) NOT NULL
                )
            """);

            // Create products table
            handle.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    price BIGINT NOT NULL,
                    size VARCHAR(50),
                    color VARCHAR(50),
                    material VARCHAR(100),
                    virtual BOOLEAN NOT NULL,
                    listed BOOLEAN NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create stock table
            handle.execute("""
                CREATE TABLE IF NOT EXISTS stock (
                    id BIGSERIAL PRIMARY KEY,
                    product_id BIGINT NOT NULL REFERENCES products(id),
                    quantity BIGINT NOT NULL,
                    discount DOUBLE PRECISION NOT NULL
                )
            """);

            // Create orders table
            handle.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id BIGSERIAL PRIMARY KEY,
                    uuid UUID NOT NULL,
                    cart JSONB NOT NULL,
                    destination VARCHAR(255) NOT NULL,
                    phone VARCHAR(50),
                    email VARCHAR(255),
                    status VARCHAR(50) NOT NULL,
                    transaction VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create users table
            handle.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    uuid UUID PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    roles VARCHAR(50)[] NOT NULL,
                    stats JSONB NOT NULL
                )
            """);
        });
    }
}