package com.motadata;

import com.motadata.api.APIServer;
import com.motadata.constants.Constants;
import com.motadata.db.Database;
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
        Database.createDatabase(Constants.CREDENTIAL_ROUTE);

        Database.createDatabase(Constants.DISCOVERY_ROUTE);

        vertx.deployVerticle(APIServer.class.getName())
                .compose(future-> vertx.deployVerticle(PollingEngine.class.getName())
                        .onSuccess(event -> LOGGER.info("API Server and Polling Engine deployed successfully"))
                        .onFailure(event -> LOGGER.info("API Server and Polling Engine deployment failed: {}", event.getMessage())));
    }
}
