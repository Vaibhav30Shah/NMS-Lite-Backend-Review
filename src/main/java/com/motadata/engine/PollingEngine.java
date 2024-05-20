package com.motadata.engine;

import com.motadata.constants.Constants;
import com.motadata.db.DatabaseUtils;
import com.motadata.util.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class PollingEngine extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingEngine.class);

    @Override
    public void start(Promise<Void> startPromise)
    {
        var pollingTime = Constants.POLLING_INTERVAL * 1000;

        vertx.setPeriodic(pollingTime, id ->
        {
            // Get the map of provisioned discovery profiles
            var provisionedDiscoveryIds = DatabaseUtils.getPollingQualifiedProfiles(); //limitation add that deleting disc profile will stop provisioning and polling

            if (!provisionedDiscoveryIds.isEmpty())
            {
                System.out.println(provisionedDiscoveryIds);

                for (var entry : provisionedDiscoveryIds.entrySet())
                {
                    fetchData(entry.getValue(), entry.getKey());
                }
            }
            else
            {
                LOGGER.info("No provisioned discovery profiles found.");
            }
        });
    }

    private void fetchData(JsonObject discoveryProfile, int discoveryProfileId)
    {
        vertx.executeBlocking(event ->
                        {
                            try
                            {
                                var discoveryProfileJson = new JsonArray();

                                discoveryProfileJson.add(discoveryProfile);

                                String encodedJson = Base64.getEncoder().encodeToString(discoveryProfileJson.encode().getBytes());

                                var decodedString = Util.executeProcess(encodedJson);

                                Util.dumpData(discoveryProfile.getString("ip"), decodedString);

                                LOGGER.info("Received data for discovery profile ID: {}", discoveryProfileId);

                                LOGGER.info("Polled Data: {}", new JsonArray(decodedString).encode());
                            }
                            catch (Exception e)
                            {
                                LOGGER.error("Error in fetching data from plugin engine for Discovery Profile ID {}: {}", discoveryProfileId, e.getMessage());
                            }
                        }
                ).onSuccess(future -> LOGGER.info("Discovery Profile ID {} fetched successfully", discoveryProfileId))
                .onFailure(future -> LOGGER.info("Discovery Profile ID {} fetching failed", discoveryProfileId));
    }
}
