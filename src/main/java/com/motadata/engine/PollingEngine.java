package com.motadata.engine;

import com.motadata.constants.Constants;
import com.motadata.db.DiscoveryDatabase;
import com.motadata.util.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class PollingEngine extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingEngine.class);

    private static final DiscoveryDatabase discoveryDatabase = new DiscoveryDatabase();

    @Override
    public void start(Promise<Void> startPromise)
    {
        LOGGER.info("Starting Scheduling Engine");

        vertx.setPeriodic(Constants.POLLING_INTERVAL*1000, id ->
        {
            var discoveryProfiles = discoveryDatabase.get();

            var provisionedDiscoveryProfiles = new JsonArray();

            for (Object profile : discoveryProfiles)
            {
                if (profile instanceof JsonObject jsonProfile && jsonProfile.containsKey("is.provisioned"))
                {
                    provisionedDiscoveryProfiles.add(jsonProfile);
                }
            }

            if (!provisionedDiscoveryProfiles.isEmpty())
            {
                sendProvisionedProfiles(provisionedDiscoveryProfiles);
            }
        });
    }

    private void sendProvisionedProfiles(JsonArray provisionedDiscoveryProfiles)
    {
        try (ZContext context = new ZContext())
        {
            vertx.executeBlocking(event ->
            {
                ZMQ.Socket requester = context.createSocket(SocketType.REQ);

                requester.connect(Constants.ZMQ_ADDRESS);

                requester.send(provisionedDiscoveryProfiles.encode().getBytes(), 0);

                var reply = requester.recv(0);

                var encryptedData = new String(reply);

                LOGGER.info("Received encrypted data from poller: {}", encryptedData);

                processEncryptedData(encryptedData, provisionedDiscoveryProfiles);
            });
        }
        catch (Exception e)
        {
            LOGGER.error("Error in communicating with poller: {}", e.getMessage());
        }
    }

    private void processEncryptedData(String encryptedData, JsonArray provisionedDiscoveryProfiles)
    {
        try
        {
            // Decrypt the data
            var decodedData = Base64.getDecoder().decode(encryptedData);

            var aesKey = new SecretKeySpec(Constants.AES_KEY.getBytes(), "AES");

            var cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, aesKey);

            var decryptedData = cipher.doFinal(decodedData);

            var decodedString = new String(decryptedData);

            if (decodedString != null)
            {
                for (var profile : provisionedDiscoveryProfiles)
                {
                    if (profile instanceof JsonObject jsonProfile)
                    {
                        var ip = jsonProfile.getString("ip");

                        LOGGER.info("IP: {}", ip);

                        var future = Util.dumpData(vertx, ip, decodedString);

                        if (future.succeeded())
                            LOGGER.info("Data dumped into file for IP: {}", ip);

                        if (future.failed())
                            LOGGER.warn("Data dumping failed for IP: {} \n Cause: {}", ip, future.cause().getMessage());

                        LOGGER.info("Received data for discovery profile ID: {}", jsonProfile.getString("discovery.id"));

                        LOGGER.trace("Polled Data: {}", new JsonArray(decodedString).encode());
                    }
                }
            }
            else
            {
                LOGGER.warn("No data received from poller");
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Error in decrypting data: {}", exception.getMessage());
        }
    }
}