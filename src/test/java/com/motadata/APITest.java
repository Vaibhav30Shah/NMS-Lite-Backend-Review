package com.motadata;

import com.motadata.constants.Constants;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class APITest
{
    @Test
    void testCredential_GetAPI(Vertx vertx, VertxTestContext testContext)
    {
        var client = vertx.createHttpClient();

        client.request(HttpMethod.GET, Constants.PORT, "localhost", "/" + Constants.CREDENTIAL_ROUTE + "/").compose(HttpClientRequest ->
                        HttpClientRequest.send().compose(HttpClientResponse::body)
                )
                .onComplete(testContext.succeeding(buffer -> testContext.verify(() ->
                {
                    assertFalse(buffer.toJsonArray().isEmpty());

                    testContext.completeNow();
                })));
    }

    @Test
    void testCredential_PostAPI(Vertx vertx, VertxTestContext testContext)
    {
        var client = vertx.createHttpClient();

        var obj = new JsonObject()
                .put("credential.name", "Switch 1212")
                .put("protocol", "snmp")
                .put("version", "2c")
                .put("community", "public");

        client.request(HttpMethod.POST, Constants.PORT, "localhost", "/" + Constants.CREDENTIAL_ROUTE + "/").compose(HttpClientRequest ->
                        HttpClientRequest.send(obj.encode()).compose(HttpClientResponse::body)
                )
                .onComplete(testContext.succeeding(buffer -> testContext.verify(() ->
                {
                    assertEquals(200, buffer.toJsonObject().getInteger(Constants.STATUS));

                    testContext.completeNow();
                })));
    }

}
