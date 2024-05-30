package com.motadata.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import com.motadata.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIServer extends AbstractVerticle
{
    private final Logger LOGGER = LoggerFactory.getLogger(APIServer.class);

    @Override
    public void start(Promise<Void> startPromise)
    {
        var server = vertx.createHttpServer();

        var router = Router.router(vertx);

        var getPollingData = new GetHistoricalData(vertx);

        // Main routing
        router.route("/").handler(routingContext ->
                routingContext.response().end("Welcome to NMS Lite"));

        // Credential Profile route
        router.route("/" + Constants.CREDENTIAL_ROUTE + "/*").subRouter(Credential.getRouter(vertx));

        // Discovery Profile route
        router.route("/" + Constants.DISCOVERY_ROUTE + "/*").subRouter(Discovery.getRouter(vertx));

        // Provisioning route
        router.route("/" + Constants.PROVISION_ROUTE + "/*").subRouter(Provision.getRouter(vertx));

        // Polling Data route
        router.route("/"+Constants.HISTORICAL_DATA_ROUTE+"/:ip").handler(getPollingData::handleGetPollingData);

        server.requestHandler(router).listen(Constants.PORT)
                .onSuccess(event ->
                {
                    startPromise.complete();

                    LOGGER.info("API Server started on port {}", Constants.PORT);
                })
                .onFailure(event ->
                {
                    startPromise.fail(event);

                    LOGGER.warn("API Server failed to start on port {}", Constants.PORT);
                });
    }
}
