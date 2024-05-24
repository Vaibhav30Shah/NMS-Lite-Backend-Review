package com.motadata;

import com.motadata.api.APIServer;
import com.motadata.engine.DiscoveryEngine;
import com.motadata.engine.PollingEngine;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    private static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args)
    {
        vertx.deployVerticle(APIServer.class.getName())
                .compose(compositeFuture -> vertx.deployVerticle(DiscoveryEngine.class.getName()))
                .compose(future -> vertx.deployVerticle(PollingEngine.class.getName())
                        .onSuccess(event -> LOGGER.info("API Server, Polling Engine and Scheduling Engine deployed successfully"))
                        .onFailure(event -> LOGGER.info("API Server, Polling Engine and Scheduling Engine deployment failed: {}", event.getMessage())));
    }
}
