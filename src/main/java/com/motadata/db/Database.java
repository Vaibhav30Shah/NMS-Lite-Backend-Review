package com.motadata.db;

import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Database
{

    private static final Map<String, Database> instances = new ConcurrentHashMap<>();

    private static final AtomicInteger idCounter = new AtomicInteger(1);

    final Map<Integer, JsonObject> profileDetails = new ConcurrentHashMap<>();

    static final JsonArray discoveryProfiles = new JsonArray();

    static final Set<Integer> validDiscoveryIds = new ConcurrentHashSet<>();

    static final Map<Integer, JsonObject> provisionedDiscoveryIds = new ConcurrentHashMap<>();

    static final Map<Integer, JsonObject> pollingQualifiedProfile=new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    //methods for database
    public static void createDatabase(String name)
    {
        if (!instances.containsKey(name))
        {
            Database database = new Database();

            instances.put(name, database);

            LOGGER.info("Database {} created", name);
        }
        else
        {
            LOGGER.info("Database {} already exists", name);
        }
    }

    public static Database getDatabase(String name)
    {
        LOGGER.info("Getting database for {}", name);

        return instances.get(name);
    }

    //CRUD
    public int create(JsonObject profile)
    {
        var id = idCounter.getAndIncrement();

        profileDetails.put(id, profile);

        return id;
    }

    public JsonObject get()
    {
        JsonObject result = new JsonObject();

        profileDetails.forEach((key, value) -> result.put(String.valueOf(key), value));

        LOGGER.info("Get all the data has been served");

        return result.copy();
    }

    public JsonObject get(int id)
    {
        var result = profileDetails.get(id);

        if (result != null)
        {
            LOGGER.info("Get the data for id {} has been served", id);

            return result.copy();
        }
        else
        {
            LOGGER.info("No data found for id {}", id);

            return null;
        }
    }

    public boolean update(JsonObject updatedData, int id)
    {
        var previousData = profileDetails.get(id);

        var currentData = updatedData.getMap();

        var keysOfCurrentData = currentData.keySet();

        for (var key : keysOfCurrentData)
        {
            previousData.put(key, currentData.get(key));

            return true;
        }

        return false;
    }

    public boolean delete(int id)
    {
        LOGGER.info("{} removed from database.", id);

        return profileDetails.remove(id) != null;
    }
}
