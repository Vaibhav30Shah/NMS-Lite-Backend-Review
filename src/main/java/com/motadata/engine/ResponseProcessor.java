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
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Arrays;
import java.util.Base64;

public class ResponseProcessor extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseProcessor.class);

    private ZContext context;

    private ZMQ.Socket pullSocket;

    @Override
    public void start(Promise<Void> startPromise)
    {
        try
        {
            LOGGER.info("Starting Response Processor");

            context = new ZContext();

            pullSocket = context.createSocket(SocketType.PULL);

            pullSocket.bind(Constants.ZMQ_RECEIVE_ADDRESS);

            new Thread(() ->
            {
                while (true)
                {
                    var reply = pullSocket.recv(0);

                    if (reply != null)
                    {
                        LOGGER.info("Recd result: {}", new String(reply));

                        var decodedData = Base64.getDecoder().decode(reply);

                        LOGGER.info("Decoded data: {}", new String(decodedData));

                        JsonArray data = new JsonArray(new String(decodedData));

                        LOGGER.info("Received data from poller: {}", data.encode());

                        processData(data);
                    }
                    else
                    {
                        LOGGER.info("No response received");
                    }
                }
            }).start();

            startPromise.complete();
        }
        catch (Exception exception)
        {
            LOGGER.error("Error in starting response processor: ", exception);
        }
    }

    private void processData(JsonArray data)
    {
        try
        {
            LOGGER.info("Data:{}", data.encode());

            for (var profile : data)
            {
                if (profile instanceof JsonObject jsonProfile)
                {
                    LOGGER.info("JsonProfile:{}", jsonProfile);

                    if (jsonProfile.getString("plugin.type").equals("Discover") && jsonProfile.getString("error").equals("[]"))
                    {
                        LOGGER.info("Sending discovery data to event bus: {}", jsonProfile.encodePrettily());

                        DiscoveryDatabase.discoveredProfiles.put(jsonProfile.getInteger(Constants.KEY_DISCOVERY_ID), data);
                    }
                    else
                    {
                        var ip = jsonProfile.getString("ip");

                        LOGGER.info("IP: {}", ip);

                        vertx.<JsonArray>executeBlocking(handler ->
                        {
                            var future = Util.dumpData(vertx, ip, Buffer.buffer(data.encodePrettily()));

                            if (future.succeeded())
                                LOGGER.info("Data dumped into file for IP: {}", ip);

                            if (future.failed())
                                LOGGER.warn("Data dumping failed for IP: {} \n Cause: {}", ip, future.cause().getMessage());
                        });

                        LOGGER.info("Received data for discovery profile ID: {}", jsonProfile.getString("discovery.profile.id"));

                        LOGGER.trace("Polled Data: {}", new JsonArray(data.encodePrettily()));
                    }
                }
                else
                {
                    LOGGER.info("Not instance of JSON Profiles");
                }
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Error in processing data: {}", exception.toString());
        }
    }

    @Override
    public void stop()
    {
        if (context != null)
            context.close();

        if (pullSocket != null)
            pullSocket.close();
    }
}
