package com.motadata.api;

import com.motadata.Bootstrap;
import com.motadata.constants.Constants;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class GetHistoricalData
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GetHistoricalData.class);

    private final Vertx vertx= Bootstrap.getVertx();

    private final JsonArray pollingData = new JsonArray();

    public void handleGetPollingData(RoutingContext routingContext)
    {
        var ip = routingContext.pathParam("ip");

        vertx.<JsonArray>executeBlocking(promise ->
        {
            try
            {
                var pollingData = readPollingData(ip);

                promise.complete(pollingData);
            }
            catch (Exception exception)
            {
                promise.fail(exception);
            }
        }, res ->
        {
            if (res.succeeded())
            {
                routingContext.response()
                        .putHeader("Content-Type", "application/json")
                        .end(res.result().encodePrettily());
            }
            else
            {
                LOGGER.error("Failed to read polling data", res.cause());

                routingContext.fail(res.cause());
            }
        });
    }

    private JsonArray readPollingData(String ip)
    {
        try
        {
            var dirPath = Constants.POLLING_DATA_STORE + ip;

            var files = vertx.fileSystem().readDirBlocking(dirPath).stream()
                    .sorted(Comparator.reverseOrder())
                    .limit(10)
                    .toList();

            for (var file : files)
            {
                var fileData = new JsonArray(vertx.fileSystem().readFileBlocking(file).toString());

                for (var entry : fileData)
                {
                    pollingData.add(entry);
                }
            }
            return pollingData;
        }
        catch (Exception exception)
        {
            LOGGER.error("Failed to read polling data: ", exception);
        }
        return pollingData;
    }
}
