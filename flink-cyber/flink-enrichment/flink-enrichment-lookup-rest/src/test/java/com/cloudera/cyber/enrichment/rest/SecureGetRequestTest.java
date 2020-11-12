package com.cloudera.cyber.enrichment.rest;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class SecureGetRequestTest extends GetRestRequestTest {

    @BeforeClass
    public static void createMockService() {
        createMockService(true);
    }

    @AfterClass
    public static void stopMockServer() {
        mockRestServer.close();
    }

}
