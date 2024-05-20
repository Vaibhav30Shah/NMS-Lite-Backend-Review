package com.motadata.api;

import com.motadata.constants.Constants;
import com.motadata.db.Database;
import com.motadata.util.Util;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class Credential
{
    static Database database=Database.getDatabase(Constants.CREDENTIAL_ROUTE);

    public static Router getRouter(Vertx vertx)
    {
        var router = Router.router(vertx);

        // Create credential profile
        router.post("/").handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                database.create(body.toJsonObject());

                routingContext.response()
                        .setStatusCode(Constants.SUCCESS_STATUS)
                        .putHeader("Content-Type", "Text/plain")
                        .end("Credential Profile created successfully");
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        }));

        // Get particular credentials
        router.get("/:" + Constants.CREDENTIAL_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                var credentialProfile = database.get(Integer.parseInt(routingContext.request().getParam(Constants.CREDENTIAL_PROFILE_ID)));

                if (credentialProfile != null)
                {
                    routingContext.response()
                            .putHeader("Content-Type", "application/json")
                            .end(credentialProfile.encode());
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Credential profile not found");
                }
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        });

        // Get all credentials
        router.get("/").handler(routingContext ->
        {
            try
            {
                var allProfiles = database.get();

                routingContext.response()
                        .putHeader("Content-Type", "application/json")
                        .end(database.get().encode());
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        });

        // Update credential
        router.put("/:" + Constants.CREDENTIAL_PROFILE_ID).handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                int credProfileId = Integer.parseInt(routingContext.request().getParam(Constants.CREDENTIAL_PROFILE_ID));

                JsonObject updatedCredentialProfile = body.toJsonObject();

                updatedCredentialProfile.remove(Constants.KEY_CREDENTIAL_ID);

                if (database.update(updatedCredentialProfile, credProfileId))
                {
                    routingContext.response().setStatusCode(Constants.SUCCESS_STATUS).end("Credential Profile updated successfully");
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Credential profile not found");
                }
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        }));

        // Delete credential
        router.delete("/:" + Constants.CREDENTIAL_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                int credentialProfileId = Integer.parseInt(routingContext.request().getParam(Constants.CREDENTIAL_PROFILE_ID));

                if (database.delete(credentialProfileId))
                {
                    routingContext.response().setStatusCode(Constants.SUCCESS_STATUS).end("Credential Profile Deleted successfully");
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Credential profile not found");
                }
            }
            catch (Exception e)
            {
                Util.errorHandler(routingContext, e);
            }
        });

        return router;
    }
}