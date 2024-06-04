package com.motadata.engine;

import com.motadata.constants.Constants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static com.motadata.engine.DiscoveryEngine.discoveryDatabase;

public class Scheduler extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        vertx.setPeriodic(Constants.POLLING_INTERVAL * 1000, id ->
        {
            LOGGER.info("Scheduler interval started again");

            var discoveryProfiles = discoveryDatabase.get();

            var provisionedDiscoveryProfiles = new JsonArray();

            LOGGER.info("Discovery profiles: {}", discoveryProfiles);

            for (var profile : discoveryProfiles)
            {
                if (profile instanceof JsonObject jsonProfile && jsonProfile.containsKey("is.provisioned"))
                {
                    provisionedDiscoveryProfiles.add(jsonProfile);
                }
            }

            LOGGER.info("Provisioned Discovery profiles: {}", provisionedDiscoveryProfiles);

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
            var provisionedProfiles = Base64.getEncoder().encode(provisionedDiscoveryProfiles.encode().getBytes());

            LOGGER.info("Encoded provisioned profiles: {}", new String(provisionedProfiles));

            vertx.eventBus().send(Constants.DATA_SEND_ADDRESS, new String(provisionedProfiles));
        }
        catch (Exception e)
        {
            LOGGER.error("Error in communicating with poller: {}", e.getMessage());
        }
    }
}
