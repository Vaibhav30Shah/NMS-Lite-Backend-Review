package com.motadata.engine;

import com.motadata.constants.Constants;
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
    static DiscoveryDatabase discoveryDatabase = new DiscoveryDatabase();

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryEngine.class);

    @Override
    public void start(Promise<Void> startPromise)
    {
        vertx.eventBus().<Integer>localConsumer(Constants.PING_CHECK_ADDRESS, message ->
        {
            var discoveryID = message.body();

            var discoveryProfile = discoveryDatabase.get(discoveryID);

            var ip = discoveryProfile.getString("ip");

            var data = new JsonArray();

            vertx.<JsonArray>executeBlocking(promise ->
            {
                try
                {
                    if (Util.isPingSuccessful(ip))
                    {
                        LOGGER.info("PING Check Successful {}", ip);

                        discoveryProfile.put("plugin.type", "Discover");

                        discoveryProfile.put("is.discovered", true);

                        data.add(discoveryProfile);

                        promise.complete(data);
                    }
                    else
                    {
                        promise.fail("The Specified device is not alive");
                    }
                }
                catch (Exception exception)
                {
                    LOGGER.error("Error in running discovery: {}", exception.getMessage());

                    promise.fail(exception);
                }
            }, false, asyncHandler ->
            {
                if (asyncHandler.succeeded())
                {
                    vertx.eventBus().send(Constants.RUN_DISCOVERY_ADDRESS, Base64.getEncoder().encodeToString(asyncHandler.result().encode().getBytes()));
                }
                else
                {
                    LOGGER.warn("The specified device is not alive");
                }
            });
        });
        startPromise.complete();
    }
}