package com.motadata.engine;

import com.motadata.constants.Constants;
import com.motadata.db.Database;
import com.motadata.db.DiscoveryDatabase;
import com.motadata.util.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class DiscoveryEngine extends AbstractVerticle
{
    static DiscoveryDatabase discoveryDatabase=new DiscoveryDatabase();

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryEngine.class);

    @Override
    public void start(Promise<Void> startPromise)
    {
        vertx.eventBus().localConsumer(Constants.RUN_DISCOVERY_ADDRESS, message ->
        {
            var discoveryID = (int) message.body();

            var discoveryProfile = discoveryDatabase.get(discoveryID);

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

                                discoveryProfile.put("is.discovered", true);

                                // Encode the JSON and send it to the plugin engine
                                var decodedString = Util.executeProcess(Base64.getEncoder().encodeToString(discoveryProfile.encode().getBytes()));

                                if(decodedString!=null)
                                {
                                    var outputArray = new JsonArray();

                                    outputArray.addAll(new JsonArray(decodedString));

                                    discoveryProfile.put("is.valid",true);

                                    promise.complete(outputArray);
                                }
                                else
                                {
                                    promise.fail("Something went wrong");
                                }
                            }
                            else
                            {
                                promise.fail("The Specified device is not alive");

                                LOGGER.warn("The specified device is not alive");
                            }
                        }
                        catch (Exception exception)
                        {
                            LOGGER.error("Error in running discovery: {}", exception.getMessage());

                            promise.fail(exception);
                        }
                    }
                    , result ->
                    {
                        if (result.succeeded())
                        {
                            var outputArray = result.result();

                            // Store the outputArray for later retrieval
                            Database.discoveryResults.put(discoveryID, outputArray);

                            LOGGER.trace("Discovery Result: {}", outputArray);
                        }
                        else
                        {
                            LOGGER.error("Error in running discovery: {}", result.cause().getMessage());
                        }
                    });
        });
        startPromise.complete();
    }
}