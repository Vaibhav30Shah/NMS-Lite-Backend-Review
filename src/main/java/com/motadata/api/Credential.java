package com.motadata.api;

import com.motadata.constants.Constants;
import com.motadata.db.CredentialDatabase;
import com.motadata.db.Database;
import com.motadata.db.DiscoveryDatabase;
import com.motadata.util.Util;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class Credential
{
    static CredentialDatabase credentialDatabase=new CredentialDatabase();

    public static Router getRouter(Vertx vertx)
    {
        var router = Router.router(vertx);

        // Create credential profile
        router.post("/").handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                var credential = body.toJsonObject();

                if (Util.validateCredential(credential.getString("community"), credential.getString("version")))
                {
                    credentialDatabase.create(credential);

                    Util.successHandler(routingContext, "Credential profile created successfully");
                }
                else
                {
                    Util.errorHandler(routingContext, Constants.BAD_REQUEST_STATUS, "Invalid credential");
                }
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        }));

        // Get particular credentials
        router.get("/:" + Constants.CREDENTIAL_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                var credentialProfile = credentialDatabase.get(Integer.parseInt(routingContext.request().getParam(Constants.CREDENTIAL_PROFILE_ID)));

                if (credentialProfile != null)
                {
                    routingContext.response()
                            .putHeader("Content-Type", "application/json")
                            .end(credentialProfile.encode());
                }
                else
                {
                    Util.errorHandler(routingContext, Constants.NOT_FOUND_STATUS, "Credential profile not found");
                }
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        });

        // Get all credentials
        router.get("/").handler(routingContext ->
        {
            try
            {
                routingContext.response()
                        .putHeader("Content-Type", "application/json")
                        .end(credentialDatabase.get().encode());
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        });

        // Update credential
        router.put("/:" + Constants.CREDENTIAL_PROFILE_ID).handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                var updatedCredentialProfile = body.toJsonObject();

                if (Util.validateCredential(updatedCredentialProfile.getString("community"), updatedCredentialProfile.getString("version")))
                {
                    updatedCredentialProfile.remove(Constants.KEY_CREDENTIAL_ID);

                    if (credentialDatabase.update(updatedCredentialProfile, Integer.parseInt(routingContext.request().getParam(Constants.CREDENTIAL_PROFILE_ID))))
                    {
                        Util.successHandler(routingContext, "Credential profile updated successfully");
                    }
                    else
                    {
                        Util.errorHandler(routingContext, Constants.NOT_FOUND_STATUS, "Credential profile in use or not found");
                    }
                }
                else
                {
                    Util.errorHandler(routingContext, Constants.BAD_REQUEST_STATUS, "Invalid credential");
                }
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        }));

        // Delete credential
        router.delete("/:" + Constants.CREDENTIAL_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                //TODO:left
                var discovery = new DiscoveryDatabase();

                var cred = new CredentialDatabase();

                var discoveryProfiles = discovery.get();

//                var credProfile = discoveryProfiles.getJsonArray(Constants.CREDENTIAL_PROFILE);

                for (var discoveryProfile : discoveryProfiles)
                {

                }

                if (credentialDatabase.delete(Integer.parseInt(routingContext.request().getParam(Constants.CREDENTIAL_PROFILE_ID))))
                {
                    Util.successHandler(routingContext, "Credential profile deleted successfully");
                }
                else
                {
                    Util.errorHandler(routingContext, Constants.NOT_FOUND_STATUS, "Credential profile in use or not found");
                }
            }
            catch (Exception exception)
            {
                Util.exceptionHandler(routingContext, exception);
            }
        });

        return router;
    }
}