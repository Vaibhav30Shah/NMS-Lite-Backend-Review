package com.motadata.db;

import com.motadata.constants.Constants;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscoveryDatabase implements Database
{
    private static final Map<Integer, JsonObject> discoveries = new ConcurrentHashMap<>();

    @Override
    public int create(JsonObject discovery)
    {
        var ip = discovery.getString("ip");

        for (JsonObject existingDiscovery : discoveries.values())
        {
            if (existingDiscovery.getString("ip").equals(ip))
            {
                return -1;
            }
        }

        var id = idCounter.getAndIncrement();

        discovery.put(Constants.KEY_DISCOVERY_ID, id);

        discoveries.put(id, discovery);

        return id;
    }

    @Override
    public JsonObject get(int id)
    {
        return discoveries.get(id);
    }

    @Override
    public JsonArray get()
    {
        var result = new JsonArray();

        discoveries.forEach((key, value) -> result.add(value.copy()));

        return result;
    }

    @Override
    public boolean update(JsonObject updatedDiscovery, int id)
    {
        if (discoveries.containsKey(id) && !discoveries.get(id).containsKey("is.provisioned"))
        {
            discoveries.put(id, updatedDiscovery);

            return true;
        }
        return false;
    }

    @Override
    public boolean delete(int id)
    {
        if (discoveries.containsKey(id) && !discoveries.get(id).containsKey("is.provisioned"))
        {
            discoveries.get(id).getJsonArray("credetial.profile").add(new JsonObject().put("is.bound",false));

            discoveries.remove(id);

            return true;
        }
        return false;
    }
}
