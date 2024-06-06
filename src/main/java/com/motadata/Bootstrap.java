package com.motadata;

import com.motadata.api.APIServer;
import com.motadata.engine.*;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    private static final Vertx vertx = Vertx.vertx();

    public static Vertx getVertx()
    {
        return vertx;
    }

    public static void main(String[] args)
    {
        vertx.deployVerticle(APIServer.class.getName())
                .compose(future -> vertx.deployVerticle(DiscoveryEngine.class.getName()))
                .compose(future -> vertx.deployVerticle(Scheduler.class.getName()))
                .compose(future -> vertx.deployVerticle(Poller.class.getName()))
                .compose(future -> vertx.deployVerticle(ResponseReceiver.class.getName()))
                .compose(future->vertx.deployVerticle(ResponseProcessor.class.getName()))
                .onSuccess(event -> LOGGER.info("Everything deployed successfully"))
                .onFailure(event -> LOGGER.info("Deployment failed: {}", event.getCause().toString()));
    }
}
