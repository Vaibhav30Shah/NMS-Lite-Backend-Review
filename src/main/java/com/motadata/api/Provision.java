package com.motadata.api;

import com.motadata.constants.Constants;
import com.motadata.db.Database;
import com.motadata.db.DatabaseUtils;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Provision
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Provision.class);

    public static Router getRouter(Vertx vertx)
    {
        Router router = Router.router(vertx);

        // Provisioning device route
        router.post("/:"+Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                int discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                if (DatabaseUtils.isValidDiscoveryId(discoveryProfileId))
                {
                    var discoveryProfile=Database.getDatabase(Constants.DISCOVERY_ROUTE).get(discoveryProfileId);

                    if (discoveryProfile != null)
                    {
                        discoveryProfile.put("plugin.type","Collect");

                        DatabaseUtils.provisionDiscoveryId(discoveryProfileId, discoveryProfile);

                        LOGGER.info("Discovery Profile Updated Successfully");

                        LOGGER.info("Provisioned Discovery IDs: {}", DatabaseUtils.getProvisionedDiscoveryIds());

                        routingContext.response().setStatusCode(Constants.SUCCESS_STATUS).end("Discovery Profile Provisioned Successfully");
                    }
                    else
                    {
                        LOGGER.warn("Discovery profile not found for ID: {}", discoveryProfileId);
                    }
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.BAD_REQUEST_STATUS).end("Invalid discovery profile ID.");
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Error during provisioning process:", e);

                routingContext.response().setStatusCode(Constants.ERROR_STATUS).end("Error during provisioning process.");
            }
        });

        return router;
    }
}