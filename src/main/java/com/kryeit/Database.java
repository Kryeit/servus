package com.kryeit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.kryeit.auth.JsonObjectMapper;
import com.kryeit.auth.User;
import com.kryeit.cosmetics.CosmeticApi;
import com.kryeit.merch.Order;
import com.kryeit.merch.Product;
import com.kryeit.merch.Stock;
import com.kryeit.panel.auth.Admin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class Database {
    private static final Jdbi JDBI;

    static {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(Config.dbUser);
        hikariConfig.setPassword(Config.dbPassword);
        hikariConfig.setJdbcUrl(Config.dbUrl);


        JDBI = Jdbi.create(new HikariDataSource(hikariConfig));

        JDBI.registerRowMapper(ConstructorMapper.factory(User.Data.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(Order.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(Product.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(Stock.class));

        //JDBI.registerRowMapper(ConstructorMapper.factory(CosmeticApi.CosmeticData.class));
        //        JDBI.registerRowMapper(ConstructorMapper.factory(CosmeticApi.WardrobeItem.class));
        //        JDBI.registerRowMapper(ConstructorMapper.factory(CosmeticApi.EquippedCosmeticsEntry.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(Admin.class));
        JDBI.registerColumnMapper(Long[].class, new ColumnMapper<Long[]>() {
            @Override
            public Long[] map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
                Array sqlArray = rs.getArray(columnNumber);
                if (sqlArray == null) {
                    return new Long[0];
                }

                Object[] array = (Object[]) sqlArray.getArray();
                Long[] result = new Long[array.length];

                for (int i = 0; i < array.length; i++) {
                    if (array[i] != null) {
                        if (array[i] instanceof Number) {
                            result[i] = ((Number) array[i]).longValue();
                        } else {
                            result[i] = Long.parseLong(array[i].toString());
                        }
                    }
                }

                return result;
            }
        });
        JDBI.registerColumnMapper(JsonObject.class, new JsonObjectMapper());
        DatabaseUtils.createTables();

        JDBI.installPlugin(new Jackson2Plugin());
    }

    public static Jdbi getJdbi() {
        return JDBI;
    }
}