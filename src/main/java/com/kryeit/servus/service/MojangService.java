package com.kryeit.servus.service;

import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

@Service
public class MojangService {

    private static final String MOJANG_API_PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String MOJANG_API_UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";

    public String getMinecraftUsername(UUID uuid) {
        RestTemplate restTemplate = new RestTemplate();
        String url = MOJANG_API_PROFILE_URL + uuid.toString().replace("-", "");

        try {
            MojangProfile profile = restTemplate.getForObject(url, MojangProfile.class);
            if (profile != null && profile.getName() != null) {
                return profile.getName();
            } else {
                throw new IllegalArgumentException("No username found for the provided UUID");
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("No username found for the provided UUID");
        }
    }

    public UUID getMinecraftUUID(String username) {
        RestTemplate restTemplate = new RestTemplate();
        String url = MOJANG_API_UUID_URL + username;

        try {
            MojangUUIDProfile profile = restTemplate.getForObject(url, MojangUUIDProfile.class);
            if (profile != null && profile.getId() != null) {
                return UUID.fromString(profile.getId().replaceAll(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"
                ));
            } else {
                throw new IllegalArgumentException("No UUID found for the provided username");
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("No UUID found for the provided username");
        }
    }

    @Getter
    private static class MojangProfile {
        private String name;

    }

    @Getter
    private static class MojangUUIDProfile {
        private String id;

    }
}
