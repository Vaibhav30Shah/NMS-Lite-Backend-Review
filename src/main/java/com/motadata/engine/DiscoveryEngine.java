package com.motadata.engine;

import com.motadata.constants.Constants;
import com.motadata.util.Util;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Set;

public class DiscoveryEngine
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryEngine.class); //log level in src

    public static void runDiscovery(JsonObject discoveryProfile, JsonArray discoveryProfiles, Set<Integer> validDiscoveryIds, int discoveryProfileId, RoutingContext routingContext, Vertx vertx)
    {
        var ip = discoveryProfile.getString("ip");

        //execBlocking because the process can be long and can block the event loop
        vertx.<JsonArray>executeBlocking(promise ->
        {
            try
            {
                // Check if the IP is alive using fping
                if (Util.isPingSuccessful(ip))
                {
                    LOGGER.info("PING Check Successful {}", ip);

                    discoveryProfile.put("plugin.type", "Discover");

                    discoveryProfiles.add(discoveryProfile);

                    // Encode the JSON and send it to the plugin engine
                    var encodedJson = Base64.getEncoder().encodeToString(discoveryProfiles.encode().getBytes());

                    var decodedString= Util.executeProcess(encodedJson);

                    var contextOutput = new JsonArray(decodedString);

                    var outputArray = new JsonArray();

                    outputArray.addAll(contextOutput);

                    validDiscoveryIds.add(discoveryProfileId);

                    promise.complete(outputArray);
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.BAD_REQUEST_STATUS).end("The Specified device is not alive");
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Error in running discovery: {}", e.getMessage());

                promise.fail(e);
            }
        }, result ->
        {
            if (result.succeeded())
            {
                JsonArray outputArray = result.result();

                routingContext.response()
                        .setStatusCode(Constants.SUCCESS_STATUS)
                        .putHeader("Content-Type", "application/json")
                        .end(outputArray.encode());
            }
            else
            {
                routingContext.response().setStatusCode(Constants.ERROR_STATUS).end("Error in running discovery: " + result.cause().getMessage());
            }
        });
    }
}