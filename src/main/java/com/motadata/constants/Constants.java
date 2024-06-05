package com.motadata.constants;

public class Constants
{
    /* Server Port */
    public static final int PORT = 1428;

    /* Status and codes */
    public static final String STATUS = "status";

    public static final int SUCCESS_STATUS = 200;

    public static final int ERROR_STATUS = 500;

    public static final int NOT_FOUND_STATUS = 404;

    public static final int BAD_REQUEST_STATUS = 400;

    /* Error and success */
    public static final String ERROR_MESSAGE = "error.message";

    public static final String SUCCESS_MESSAGE = "success.message";

    /* Credentials */
    public static final String CREDENTIAL_PROFILE_ID = "credentialProfileId";

    public static final String CREDENTIAL_ROUTE = "credential-profile";

    public static final String CREDENTIAL_PROFILE = "credential.profile";

    public static final String KEY_CREDENTIAL_ID = "credential.profile.id";

    /* Discoveries */
    public static final String KEY_DISCOVERY_ID = "discovery.profile.id";

    public static final String DISCOVERY_ROUTE = "discovery-profile";

    public static final String DISCOVERY_PROFILE_ID = "discoveryProfileId";

    public static final String RUN_DISCOVERY = "run-discovery/";

    /* Provision */
    public static final String PROVISION_ROUTE = "provision-device";

    /* Historical Data */
    public static final String HISTORICAL_DATA_ROUTE = "api/polling-data";

    /* Paths */
    public static final String PLUGIN_ENGINE_PATH = "/home/vaibhav/GolandProjects/NMS-Lite/plugin-engine/NMS-Lite";

    public static final String POLLING_DATA_STORE = "/home/vaibhav/IdeaProjects/NMS-Lite-Backend/files/";

    /* Time intervals */
    public static final int POLLING_INTERVAL = 30;

    public static final long WAITING_TIME = 60;

    /* Event bus addresses */
    public static final String RUN_DISCOVERY_ADDRESS = "run.discovery.address";

    public static final String PING_CHECK_ADDRESS = "ping.check.address";

    public static final String DATA_SEND_ADDRESS="data.send.address";

    public static final String RESPONSE_PROCESS_ADDRESS="response.process.address";

    /* AES Key */
    public static final String AES_KEY = "465DA7D9A240D75A561C084D3509BEF0";

    /* ZMQ Addresses */
    public static final String ZMQ_SEND_ADDRESS = "tcp://*:5555";

    public static final String ZMQ_RECEIVE_ADDRESS="tcp://*:5556";
}
