package com.motadata.api;

import com.motadata.constants.Constants;
import com.motadata.db.DiscoveryDatabase;
import com.motadata.util.Util;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Provision
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Provision.class);

    private final static DiscoveryDatabase discoveryDatabase = new DiscoveryDatabase();

    public static Router getRouter(Vertx vertx)
    {
        Router router = Router.router(vertx);

        // Provisioning device route
        router.post("/:" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                var discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                var discoveryProfile = discoveryDatabase.get(discoveryProfileId);

                if (discoveryProfile.containsKey("is.discovered"))
                {
                    if (!discoveryProfile.isEmpty())
                    {
                        discoveryProfile.put("plugin.type", "Collect");

                        discoveryProfile.put("is.provisioned",true);

                        LOGGER.info("Discovery Profile Updated Successfully");

                        LOGGER.trace("Provisioned Discovery IDs: {}", discoveryProfile);

                        Util.successHandler(routingContext,"Discovery Profile Provisioned Successfully");
                    }
                    else
                    {
                        LOGGER.warn("Discovery profile not found for ID: {}", discoveryProfileId);
                    }
                }
                else
                {
                    Util.errorHandler(routingContext, Constants.BAD_REQUEST_STATUS, "Invalid discovery profile ID");
                }
            }
            catch (Exception exception)
            {
                LOGGER.error("Error during provisioning process:", exception);

                Util.errorHandler(routingContext, Constants.ERROR_STATUS, "Error during provisioning process");
            }
        });

        return router;
    }
}