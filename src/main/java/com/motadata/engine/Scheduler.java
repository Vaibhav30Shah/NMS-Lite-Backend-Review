package com.motadata.engine;

import com.motadata.constants.Constants;
import com.motadata.db.DiscoveryDatabase;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class Scheduler extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    private static final DiscoveryDatabase discoveryDatabase = new DiscoveryDatabase();

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var provisionedDiscoveryProfiles = new JsonArray();

        vertx.setPeriodic(Constants.POLLING_INTERVAL * 1000, id ->
        {
            LOGGER.info("Scheduler interval started again");

            provisionedDiscoveryProfiles.clear();

            var discoveryProfiles = DiscoveryDatabase.discoveredProfiles;

            LOGGER.trace("Discovery profiles: {}", discoveryProfiles);

            discoveryProfiles.forEach((key, entries) ->
            {
                if (entries.containsKey("is.provisioned"))
                    provisionedDiscoveryProfiles.add(entries);
            });

            LOGGER.trace("Provisioned Discovery profiles: {}", provisionedDiscoveryProfiles);

            if (!provisionedDiscoveryProfiles.isEmpty())
            {
                sendProvisionedProfiles(provisionedDiscoveryProfiles);
            }
        });

        startPromise.complete();
    }

    private void sendProvisionedProfiles(JsonArray provisionedDiscoveryProfiles)
    {
        try
        {
            var provisionedProfiles = Base64.getEncoder().encodeToString(provisionedDiscoveryProfiles.encode().getBytes());

            LOGGER.info("Encoded provisioned profiles: {}", provisionedProfiles);

            vertx.eventBus().send(Constants.DATA_SEND_ADDRESS, provisionedProfiles);
        }
        catch (Exception e)
        {
            LOGGER.error("Error in communicating with poller: {}", e.getMessage());
        }
    }
}
