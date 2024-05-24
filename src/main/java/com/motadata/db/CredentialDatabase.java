package com.motadata.db;

import com.motadata.constants.Constants;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CredentialDatabase implements Database
{
    protected static final Map<Integer, JsonObject> credentials = new ConcurrentHashMap<>();

    @Override
    public int create(JsonObject credential)
    {
        var id = idCounter.getAndIncrement();

        credential.put(Constants.KEY_CREDENTIAL_ID, id);

        credentials.put(id, credential);

        return id;
    }

    @Override
    public JsonObject get(int id)
    {
        return credentials.get(id);
    }

    @Override
    public JsonArray get()
    {
        var result = new JsonArray();

        credentials.forEach((key, value) -> result.add(value.copy()));

        return result;
    }

    @Override
    public boolean update(JsonObject updatedCredential, int id)
    {
        if (credentials.containsKey(id) && !credentials.get(id).containsKey("is.bound"))
        {
            credentials.put(id, updatedCredential);

            return true;
        }
        return false;
    }

    @Override
    public boolean delete(int id)
    {
        if (credentials.containsKey(id) && !credentials.get(id).containsKey("is.bound"))
        {
            credentials.remove(id);

            return true;
        }
        return false;
    }
}
