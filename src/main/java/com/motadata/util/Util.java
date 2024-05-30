package com.motadata.util;

import com.motadata.constants.Constants;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class Util
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    public static boolean isPingSuccessful(String ip) throws Exception
    {
        ArrayList<String> command = new ArrayList<>();

        command.add("fping");

        command.add(ip);

        command.add("-c");

        command.add("3");

        command.add("-q");

        command.add("-t");

        command.add("3000");

        var process = new ProcessBuilder(command).redirectErrorStream(true).start();

        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        var line = reader.readLine();

        return (((line.split(":"))[1]).split("=")[1].split(",")[0].split("/")[2]).equals("0%");
    }

    public static String executeProcess(String encodedJson) throws Exception
    {
        try
        {
            var process = new ProcessBuilder(Constants.PLUGIN_ENGINE_PATH, encodedJson).redirectErrorStream(true).start();

            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            var output = new StringBuilder();

            if (!process.waitFor(Constants.WAITING_TIME, TimeUnit.SECONDS))
            {
                process.destroyForcibly();
            }

            String line;

            while ((line = reader.readLine()) != null)
            {
                output.append(line);
            }

            var decodedString = new String(Base64.getDecoder().decode(output.toString()), StandardCharsets.UTF_8);

            LOGGER.debug("Decoded Result: {}", decodedString);

            return new JsonArray(decodedString).encode();
        }
        catch (Exception exception)
        {
            return null;
        }
    }

    public static Future<Void> dumpData(Vertx vertx, String ip, String decodedString)
    {
        Promise<Void> promise = Promise.promise();

        try
        {
            var data = new JsonArray(decodedString);

            var now = LocalDateTime.now();

            var dirPath = Constants.POLLING_DATA_STORE + ip;

            var fileName = Constants.POLLING_DATA_STORE + ip + "/" + now + ".txt";

            var buffer = Buffer.buffer(data.encodePrettily());

            var fileSystem = vertx.fileSystem();

            fileSystem.mkdirs(dirPath, result ->
            {
                if (result.succeeded())
                {
                    fileSystem.openBlocking(fileName, new OpenOptions().setAppend(true).setCreate(true)).write(buffer).onComplete(handler ->
                    {
                        LOGGER.info("Content written to file");

                        promise.complete();

                    }).onFailure(handler ->
                    {
                        LOGGER.warn("Error occurred while opening the file {}", handler.getCause().toString());

                        promise.fail(handler.getCause());
                    });
                }
                else
                {
                    LOGGER.warn("Error occurred while creating folder {}", result.cause().toString());

                    promise.fail(result.cause());
                }
            });

            return promise.future();
        }
        catch (Exception exception)
        {
            promise.fail(exception);
        }
        return promise.future();
    }

    public static void exceptionHandler(RoutingContext routingContext, Exception exception)
    {
        if (!routingContext.response().ended())
        {
            routingContext.json(new JsonObject().put(Constants.STATUS, Constants.ERROR_STATUS).put(Constants.ERROR_MESSAGE, exception));
        }
        else
        {
            LOGGER.error("Error: {}", exception.getMessage());
        }
    }

    public static void errorHandler(RoutingContext routingContext, int status, String error)
    {
        if (!routingContext.response().ended())
        {
            routingContext.json(new JsonObject().put(Constants.STATUS, status).put(Constants.ERROR_MESSAGE, error));
        }
        else
        {
            LOGGER.error("Error: {}", error);
        }
    }

    public static void successHandler(RoutingContext routingContext, String message)
    {
        if (!routingContext.response().ended())
        {
            routingContext.json(new JsonObject().put(Constants.STATUS, Constants.SUCCESS_STATUS).put(Constants.SUCCESS_MESSAGE, message));
        }
        else
        {
            LOGGER.error("Error in sending success response");
        }
    }

    public static boolean validateCredential(String community, String version)
    {
        if (community == null || community.isEmpty() || version == null || version.isEmpty())
        {
            return false;
        }
        return version.equalsIgnoreCase("1") || version.equalsIgnoreCase("2c") || version.equalsIgnoreCase("3");
    }

    public static boolean validateDiscovery(int port)
    {
        try
        {
            if (port <= 0 || port > 65535)
            {
                return false;
            }

            Integer.parseInt(String.valueOf(port));

            return true;
        }
        catch (NumberFormatException exception)
        {
            return false;
        }
    }
}
