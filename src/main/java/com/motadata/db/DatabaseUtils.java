package com.motadata.db;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Set;

public class DatabaseUtils
{
    public static JsonArray getDiscoveryProfileArray()
    {
        return Database.discoveryProfiles;
    }

    public static Set<Integer> getValidDiscoveryIds()
    {
        return Database.validDiscoveryIds;
    }

    // putting the provisioned discovery id in the map of provisioned ids
    public static void provisionDiscoveryId(int id, JsonObject discoveryProfile)
    {
        Database.provisionedDiscoveryIds.put(id, discoveryProfile);

        Database.pollingQualifiedProfile.put(id, discoveryProfile);
    }

    public static Map<Integer, JsonObject> getProvisionedDiscoveryIds()
    {
        return Database.provisionedDiscoveryIds;
    }

    public static Map<Integer, JsonObject> getPollingQualifiedProfiles()
    {
        return Database.pollingQualifiedProfile;
    }

    public static boolean isValidDiscoveryId(int id)
    {
        return Database.validDiscoveryIds.contains(id);
    }
}
