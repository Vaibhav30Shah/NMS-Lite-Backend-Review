package com.motadata.engine;

import com.motadata.constants.Constants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Poller extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Poller.class);

    private ZContext context;

    private ZMQ.Socket pushSocket;

    @Override
    public void start(Promise<Void> startPromise)
    {
        try
        {
            LOGGER.info("Starting Scheduling Engine");

            context = new ZContext();

            pushSocket = context.createSocket(SocketType.PUSH);

            pushSocket.bind(Constants.ZMQ_SEND_ADDRESS);

            //for run-discovery
            vertx.eventBus().<String>localConsumer(Constants.RUN_DISCOVERY_ADDRESS, message ->
            {
                try
                {
                    LOGGER.trace("Discovery received for send: {}", message.body());

                    pushSocket.send(message.body(), 0);
                }
                catch (Exception exception)
                {
                    LOGGER.error("Error in sending discovery data to plugin engine: ", exception);
                }
            });

            //for polling
            vertx.eventBus().<String>localConsumer(Constants.DATA_SEND_ADDRESS, message ->
            {
                try
                {
                    LOGGER.trace("Collect received for send: {}", message.body());

                    pushSocket.send(message.body(),0);

                    LOGGER.info("Collect Sent");
                }
                catch (Exception exception)
                {
                    LOGGER.error("Error in sending polling data to plugin engine: ", exception);
                }
            });

            startPromise.complete();
        }
        catch (Exception exception)
        {
            LOGGER.error("Error starting Polling Engine: ", exception);
        }
    }

    @Override
    public void stop()
    {
        if (context != null)
            context.close();

        if (pushSocket != null)
            pushSocket.close();
    }
}
