package com.motadata.util;

import com.motadata.constants.Constants;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class Util
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    //check for aliveness of snmp object
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
        var process = new ProcessBuilder(Constants.PLUGIN_ENGINE_PATH, encodedJson).redirectErrorStream(true).start();

        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        var output = new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null)
        {
            output.append(line);
        }

        var decodedString = new String(Base64.getDecoder().decode(output.toString()), StandardCharsets.UTF_8);

        LOGGER.debug("Decoded Result: {}", decodedString);

        if (!process.waitFor(Constants.WAITING_TIME, TimeUnit.SECONDS))
        {
            process.destroyForcibly();
        }

        return decodedString;
    }

    public static void dumpData(String ipAddress, String data)
    {
        try
        {
            Files.write(Paths.get("/home/vaibhav/IdeaProjects/NMS-Lite-Backend/files/"+ipAddress+".txt"), data.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing data to file for IP address {}: {}", ipAddress, e.getMessage());
        }
    }

    public static void errorHandler(RoutingContext routingContext, Exception e)
    {
        routingContext.response().setStatusCode(Constants.ERROR_STATUS).end("Error: " + e.getMessage());
    }
}
