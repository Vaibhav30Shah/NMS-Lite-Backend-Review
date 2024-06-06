package com.motadata.engine;

import com.motadata.constants.Constants;
import com.motadata.db.DiscoveryDatabase;
import com.motadata.util.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class ResponseProcessor extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseProcessor.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        try
        {
            vertx.eventBus().<String>localConsumer(Constants.RESPONSE_PROCESS_ADDRESS, resultHandler ->
            {
                LOGGER.info("Data:{}", resultHandler.body());

                var decodedData = new String(Base64.getDecoder().decode(resultHandler.body()));

                LOGGER.info("Decoded data: {}", decodedData);

                var data = new JsonArray(decodedData);

                LOGGER.info("Received data from poller: {}", data.encode());

                for (var profile : data)
                {
                    if (profile instanceof JsonObject jsonProfile)
                    {
                        LOGGER.trace("JsonProfile:{}", jsonProfile);

                        if (jsonProfile.getString("plugin.type").equals("Discover") && jsonProfile.getString("error").equals("[]"))
                        {
                            LOGGER.info("Saving discovery data: {}", jsonProfile.encodePrettily());

                            DiscoveryDatabase.discoveredProfiles.put(jsonProfile.getInteger(Constants.KEY_DISCOVERY_ID), jsonProfile);
                        }
                        else if (jsonProfile.getString("plugin.type").equals("Discover") && !jsonProfile.getString("error").equals("[]"))
                        {
                            LOGGER.info("Error in Discovery data: {}", jsonProfile.encodePrettily());
                        }
                        else if (jsonProfile.getString("plugin.type").equals("Collect") && jsonProfile.getString("error").equals("[]"))
                        {
                            var ip = jsonProfile.getString("ip");

                            LOGGER.info("IP: {}", ip);

                            vertx.executeBlocking(handler ->
                            {
                                var future = Util.dumpData(vertx, ip, Buffer.buffer(data.encodePrettily()));

                                if (future.succeeded())
                                    LOGGER.info("Data dumped into file for IP: {}", ip);

                                if (future.failed())
                                    LOGGER.warn("Data dumping failed for IP: {} \n Cause: {}", ip, future.cause().getMessage());
                            });

                            LOGGER.info("Received data for discovery profile ID: {}", jsonProfile.getString(Constants.KEY_DISCOVERY_ID));

                            LOGGER.trace("Polled Data: {}", data.encodePrettily());
                        }
                        else
                        {
                            LOGGER.error("Error in Collect data: {}", jsonProfile.encodePrettily());
                        }
                    }
                    else
                    {
                        LOGGER.info("Not instance of JSON Profiles");
                    }
                }
            });
        }
        catch (Exception exception)
        {
            LOGGER.error("Error in processing data: ", exception);
        }
    }
}
