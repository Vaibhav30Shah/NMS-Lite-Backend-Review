package com.motadata.api;

import com.motadata.constants.Constants;
import com.motadata.db.Database;
import com.motadata.db.DatabaseUtils;
import com.motadata.engine.DiscoveryEngine;
import com.motadata.util.Util;
import io.vertx.core.Vertx;
import io.vertx.core.impl.Utils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class Discovery
{
    static Database database = Database.getDatabase(Constants.DISCOVERY_ROUTE);

    public static Router getRouter(Vertx vertx)
    {
        Router router = Router.router(vertx);

        // Create a new discovery profile
        router.post("/").handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                var discoveryProfile = body.toJsonObject();

                var credentialProfileIds = discoveryProfile.getJsonArray(Constants.CREDENTIAL_PROFILE);

                var processedCredentialProfiles = new JsonArray();

                var atLeastOneProfileFound = false;

                for (Object credentialProfileObj : credentialProfileIds)
                {
                    if (credentialProfileObj instanceof JsonObject credentialProfileJson)
                    {
                        Integer id = credentialProfileJson.getInteger(Constants.KEY_CREDENTIAL_ID);

                        if (id != null)
                        {
                            var credentialDatabase = Database.getDatabase(Constants.CREDENTIAL_ROUTE);

                            JsonObject credProfile = credentialDatabase.get(id);

                            if (credProfile != null)
                            {
                                processedCredentialProfiles.add(credProfile);

                                atLeastOneProfileFound = true;

                                break;
                            }
                        }
                    }
                }

                if (atLeastOneProfileFound)
                {
                    discoveryProfile.put(Constants.CREDENTIAL_PROFILE, processedCredentialProfiles);

                    database.create(discoveryProfile);

                    routingContext.response()
                            .setStatusCode(Constants.SUCCESS_STATUS)
                            .putHeader("Content-Type", "text/plain")
                            .end("Discovery Profile Created successfully");
                }
                else
                {
                    routingContext.response()
                            .setStatusCode(Constants.NOT_FOUND_STATUS)
                            .end("None of the provided credential profiles exist.");
                }
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        }));

        // Get all discovery profiles
        router.get("/").handler(routingContext ->
        {
            try
            {
                var allProfiles = database.get();

                routingContext.response()
                        .putHeader("Content-Type", "application/json")
                        .end(allProfiles.encode());
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        });

        // Get a specific discovery profile
        router.get("/:" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                int discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                var discoveryProfile = database.get(discoveryProfileId);

                if (discoveryProfile != null)
                {
                    routingContext.response()
                            .putHeader("Content-Type", "application/json")
                            .end(discoveryProfile.encode());
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Discovery profile not found");
                }
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        });

        // Update a discovery profile
        router.put("/:" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                int discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                JsonObject updatedDiscoveryProfile = body.toJsonObject();

                JsonArray credentialProfileIds = updatedDiscoveryProfile.getJsonArray(Constants.CREDENTIAL_PROFILE, new JsonArray());

                boolean allCredentialsExist = true;

                for (Object credentialProfileObj : credentialProfileIds)
                {
                    if (credentialProfileObj instanceof JsonObject credentialProfileJson)
                    {
                        Integer credentialId = credentialProfileJson.getInteger(Constants.KEY_CREDENTIAL_ID);

                        if (credentialId != null && database.get(credentialId) == null)
                        {
                            allCredentialsExist = false;

                            break;
                        }
                    }
                }

                if (allCredentialsExist)
                {
                    if (database.update(updatedDiscoveryProfile, discoveryProfileId))
                    {
                        routingContext.response().setStatusCode(Constants.SUCCESS_STATUS).end("Discovery Profile Updated Successfully");
                    }
                    else
                    {
                        routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Discovery profile not found");
                    }
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("One or more credential profiles do not exist.");
                }
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        }));

        // Delete a discovery profile
        router.delete("/:" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                int discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                if (database.delete(discoveryProfileId))
                {
                    routingContext.response().setStatusCode(Constants.SUCCESS_STATUS).end("Discovery Profile Deleted Successfully");
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Discovery profile not found");
                }
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        });

        // Run discovery route
        router.post("/" + Constants.RUN_DISCOVERY + ":" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                int discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                var discoveryProfile = database.get(discoveryProfileId);

                if (discoveryProfile != null)
                {
                    DiscoveryEngine.runDiscovery(discoveryProfile, DatabaseUtils.getDiscoveryProfileArray(), DatabaseUtils.getValidDiscoveryIds(), discoveryProfileId, routingContext, vertx);
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Discovery profile not found.");
                }
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        });

        return router;
    }
}