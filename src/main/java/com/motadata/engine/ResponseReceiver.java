package com.motadata.engine;

import com.motadata.constants.Constants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ResponseReceiver extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseReceiver.class);

    private ZContext context;

    private ZMQ.Socket pullSocket;

    @Override
    public void start(Promise<Void> startPromise)
    {
        try
        {
            LOGGER.info("Starting Response Receiver");

            context = new ZContext();

            pullSocket = context.createSocket(SocketType.PULL);

            pullSocket.bind(Constants.ZMQ_RECEIVE_ADDRESS);

            new Thread(() ->
            {
                while (true)
                {
                    var reply = pullSocket.recvStr(0);

                    if (!reply.isEmpty())
                    {
                        LOGGER.info("Recd result: {}", reply);

                        vertx.eventBus().send(Constants.RESPONSE_PROCESS_ADDRESS, reply);
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

    @Override
    public void stop()
    {
        if (context != null)
            context.close();

        if (pullSocket != null)
            pullSocket.close();
    }
}
