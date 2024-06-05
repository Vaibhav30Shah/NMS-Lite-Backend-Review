package com.motadata.api;

import com.motadata.constants.Constants;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GetHistoricalData
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GetHistoricalData.class);

    private final Vertx vertx;

    List<JsonObject> pollingData = new ArrayList<>();

    public GetHistoricalData(Vertx vertx)
    {
        this.vertx = vertx;
    }

    public void handleGetPollingData(RoutingContext routingContext)
    {
        var ip = routingContext.pathParam("ip");

        var time = routingContext.pathParam("time");

        vertx.<List<JsonObject>>executeBlocking(promise ->
        {
            try
            {
                var pollingData = readPollingData(ip, Integer.parseInt(time));

                promise.complete(pollingData);
            }
            catch (Exception exception)
            {
                LOGGER.error("Failed to read polling data: ", exception);

                promise.fail(exception);
            }
        }, res ->
        {
            if (res.succeeded())
            {
                routingContext.response()
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonArray(res.result()).encodePrettily());
            }
            else
            {
                LOGGER.error("Failed to read polling data", res.cause());

                routingContext.fail(res.cause());
            }
        });
    }

    private List<JsonObject> readPollingData(String ip, int time)
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
                    pollingData.add(new JsonObject(entry.toString()));
                }
            }
            return pollingData;
        }

        catch (Exception exception)
        {
            LOGGER.error("Failed to read polling data: ", exception);
        }
        return new ArrayList<>();
    }
}
