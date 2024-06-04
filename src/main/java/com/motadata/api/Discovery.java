package com.motadata.api;

import com.motadata.constants.Constants;
import com.motadata.db.CredentialDatabase;
import com.motadata.db.Database;
import com.motadata.db.DiscoveryDatabase;
import com.motadata.engine.DiscoveryEngine;
import com.motadata.util.Util;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Discovery
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryEngine.class);

    static DiscoveryDatabase discoveryDatabase = new DiscoveryDatabase();

    public static Router getRouter(Vertx vertx)
    {
        Router router = Router.router(vertx);

        // Create a new discovery profile
        router.post("/").handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                var discoveryProfile = body.toJsonObject();

                if (Util.validateDiscovery(discoveryProfile.getInteger("port")))
                {
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
                                var credentialDatabase = new CredentialDatabase();

                                JsonObject credProfile = credentialDatabase.get(id);

                                if (credProfile != null)
                                {
                                    credProfile.put("is.bound", true);

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

                        int id = discoveryDatabase.create(discoveryProfile);

                        if (id == -1)
                        {
                            Util.errorHandler(routingContext, Constants.BAD_REQUEST_STATUS, "Discovery Profile with this IP already exists");
                        }
                        else
                        {
                            Util.successHandler(routingContext, "Discovery profile created successfully");
                        }
                    }
                    else
                    {
                        Util.errorHandler(routingContext, Constants.NOT_FOUND_STATUS, "None of the credential profiles exist.");
                    }
                }
                else
                {
                    Util.errorHandler(routingContext, Constants.BAD_REQUEST_STATUS, "Invalid discovery port");
                }
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        }));

        // Get all discovery profiles
        router.get("/").handler(routingContext ->
        {
            try
            {
                routingContext.response()
                        .putHeader("Content-Type", "application/json")
                        .end(discoveryDatabase.get().encode());
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        });

        // Get a specific discovery profile
        router.get("/:" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                var discoveryProfile = discoveryDatabase.get(Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID)));

                if (discoveryProfile != null)
                {
                    routingContext.response()
                            .putHeader("Content-Type", "application/json")
                            .end(discoveryProfile.encode());
                }
                else
                {
                    Util.errorHandler(routingContext, Constants.NOT_FOUND_STATUS, "Discovery profile not found");
                }
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        });

        // Update a discovery profile
        router.put("/:" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                var updatedDiscoveryProfile = body.toJsonObject();

                if (Util.validateDiscovery(updatedDiscoveryProfile.getInteger("port")))
                {
                    var credentialProfileIds = updatedDiscoveryProfile.getJsonArray(Constants.CREDENTIAL_PROFILE, new JsonArray());

                    var allCredentialsExist = true;

                    for (var credentialProfileObj : credentialProfileIds)
                    {
                        if (credentialProfileObj instanceof JsonObject credentialProfileJson)
                        {
                            var credentialId = credentialProfileJson.getInteger(Constants.KEY_CREDENTIAL_ID);

                            if (credentialId != null && discoveryDatabase.get(credentialId) == null)
                            {
                                allCredentialsExist = false;

                                break;
                            }
                        }
                    }

                    if (allCredentialsExist)
                    {
                        if (discoveryDatabase.update(updatedDiscoveryProfile, Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID))))
                        {
                            Util.successHandler(routingContext, "Discovery profile updated successfully");
                        }
                        else
                        {
                            Util.errorHandler(routingContext, Constants.NOT_FOUND_STATUS, "Discovery profile in use or not found");
                        }
                    }
                    else
                    {
                        Util.errorHandler(routingContext, Constants.NOT_FOUND_STATUS, "One or more credential profiles does not exist.");
                    }
                }
                else
                {
                    Util.errorHandler(routingContext, Constants.BAD_REQUEST_STATUS, "Invalid Discovery port");
                }
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        }));

        // Delete a discovery profile
        router.delete("/:" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                if (discoveryDatabase.delete(Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID))))
                {
                    Util.successHandler(routingContext, "Discovery profile deleted successfully");
                }
                else
                {
                    Util.errorHandler(routingContext, Constants.NOT_FOUND_STATUS, "Discovery profile in use or not found");
                }
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        });

        // Run discovery route
        router.post("/" + Constants.RUN_DISCOVERY + ":" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                var discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                if (discoveryDatabase.get(discoveryProfileId) != null)
                {
                    vertx.eventBus().send(Constants.PING_CHECK_ADDRESS, discoveryProfileId);

                    Util.successHandler(routingContext, "Your request is being processed. Kindly check on /get-run-discovery-data/id");
                }
                else
                {
                    Util.errorHandler(routingContext, Constants.NOT_FOUND_STATUS, "Discovery profile not found");
                }
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        });

        // Get discovery results
        router.get("/get-run-discovery-data/:" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                int discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                LOGGER.info("Discovery profile id: " + discoveryProfileId);

                LOGGER.info("Data: " + DiscoveryDatabase.discoveredProfiles);

                if(DiscoveryDatabase.discoveredProfiles.containsKey(discoveryProfileId))
                {
                    routingContext.response()
                            .putHeader("Content-Type", "application/json")
                            .end(DiscoveryDatabase.discoveredProfiles.get(discoveryProfileId).encodePrettily());
                }
            }
            catch (Exception exception)
            {
                LOGGER.error("error", exception);

                Util.exceptionHandler(routingContext, exception);
            }
        });

        return router;
    }
}