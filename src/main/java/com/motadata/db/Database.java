package com.motadata.db;

import com.motadata.constants.Constants;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public interface Database
{
    AtomicInteger idCounter = new AtomicInteger(1);

    Map<Integer, JsonArray> discoveryResults = new ConcurrentHashMap<>();

    //Methods
    int create(JsonObject data);

    JsonObject get(int id);

    JsonArray get();

    boolean update(JsonObject updatedData, int id);

    boolean delete(int id);
}
