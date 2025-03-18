package com.kryeit.cosmetics;

import com.kryeit.Config;
import com.kryeit.Database;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Header;
import io.javalin.http.UnauthorizedResponse;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CosmeticApi {
    public static void validateRequest(Context ctx) {
        if (!Objects.equals(ctx.header(Header.AUTHORIZATION), "Bearer " + Config.cosmeticApiSecret)) {
            throw new UnauthorizedResponse();
        }
    }

    public static void getCosmeticData(Context ctx) {
        long cosmeticId = ctx.pathParamAsClass("id", Long.class).get();

        ctx.json(Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT *
                        FROM cosmetics
                        WHERE id = :id
                        LIMIT 1
                        """)
                .bind("id", cosmeticId)
                .mapTo(CosmeticData.class)
                .first()));
    }

    public static void equipCosmetic(Context ctx) {
        UUID playerId = ctx.pathParamAsClass("player", UUID.class).get();
        long cosmeticId = ctx.bodyAsClass(EquipmentBody.class).cosmeticId();

        String cosmeticType = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT type
                        FROM cosmetics
                        WHERE id = :id
                        """)
                .bind("id", cosmeticId)
                .mapTo(String.class)
                .findFirst()).orElseThrow(() -> new BadRequestResponse("Unknown cosmetic"));

        List<Long> equippedCosmetics = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT id
                        FROM wardrobes
                                 JOIN cosmetics c on c.id = wardrobes.cosmetic_id
                        WHERE player_id = :player
                          AND type != :type
                        """)
                .bind("player", playerId)
                .bind("type", cosmeticType)
                .mapTo(Long.class)
                .list());
        equippedCosmetics.add(cosmeticId);

        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        UPDATE wardrobes
                        SET equipped = (cosmetic_id = any (:cosmetics))
                        WHERE player_id = :player
                        """)
                .bind("player", playerId)
                .bind("cosmetics", equippedCosmetics.toArray(new Long[0]))
                .execute());
        respondWithEquippedCosmetics(ctx, equippedCosmetics);
    }

    public static void unequipCosmetic(Context ctx) {
        UUID playerId = ctx.pathParamAsClass("player", UUID.class).get();
        long cosmeticId = ctx.bodyAsClass(EquipmentBody.class).cosmeticId();

        Database.getJdbi().useHandle(h -> h.createUpdate("""
                        UPDATE wardrobes
                        SET equipped = false
                        WHERE player_id = :player
                          AND cosmetic_id = :cosmetic
                        """)
                .bind("player", playerId)
                .bind("cosmetic", cosmeticId)
                .execute());

        respondWithEquippedCosmetics(ctx, playerId);
    }

    public static void getWardrobe(Context ctx) {
        UUID playerId = ctx.pathParamAsClass("player", UUID.class).get();

        ctx.json(Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT id, name, type, equipped, preview_image
                        FROM cosmetics
                                 JOIN wardrobes w on cosmetics.id = w.cosmetic_id
                        WHERE player_id = :player
                        """)
                .bind("player", playerId)
                .mapTo(WardrobeItem.class)
                .list()));
    }

    private static void respondWithEquippedCosmetics(Context ctx, UUID playerId) {
        List<Long> equippedCosmetics = Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT cosmetic_id
                        FROM wardrobes
                        WHERE player_id = :player
                          AND equipped
                        """)
                .bind("player", playerId)
                .mapTo(Long.class)
                .list());

        respondWithEquippedCosmetics(ctx, equippedCosmetics);
    }

    private static void respondWithEquippedCosmetics(Context ctx, List<Long> equippedCosmetics) {
        ctx.json(new EquippedCosmeticsResponse(equippedCosmetics));
    }

    public static void getEquippedCosmetics(Context ctx) {
        EquippedCosmeticsBody body = ctx.bodyAsClass(EquippedCosmeticsBody.class);

        ctx.json(Database.getJdbi().withHandle(h -> h.createQuery("""
                        SELECT player_id, array_agg(cosmetic_id) AS equippedCosmetics
                        FROM wardrobes
                        WHERE player_id = any (:players)
                          AND equipped
                        GROUP BY player_id
                        """)
                .bind("players", body.players())
                .mapTo(EquippedCosmeticsEntry.class)
                .collectToMap(EquippedCosmeticsEntry::playerId, EquippedCosmeticsEntry::equippedCosmetics)));
    }

    public static void addItemToWardrobe(UUID player, long cosmeticId) {
        Database.getJdbi().withHandle(h -> h.createUpdate("""
                        INSERT INTO wardrobes (player_id, cosmetic_id, equipped)
                        VALUES (:player, :cosmetic, false)
                        """)
                .bind("player", player)
                .bind("cosmetic", cosmeticId)
                .execute());
    }

    private record EquippedCosmeticsResponse(List<Long> equippedCosmetics) {
    }

    public record WardrobeItem(long id, String name, String type, boolean equipped,
                               String previewImage) {
    }

    public record CosmeticData(String model, String script, String type) {
    }

    private record EquipmentBody(long cosmeticId) {
    }

    private record EquippedCosmeticsBody(UUID[] players) {
    }

    public record EquippedCosmeticsEntry(UUID playerId, List<Long> equippedCosmetics) {
    }
}
