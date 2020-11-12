package com.cloudera.cyber.enrichment.rest;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class NonSecureGetRequestTest extends GetRestRequestTest {
    @BeforeClass
    public static void createMockService() {
        createMockService(false);
    }

    @AfterClass
    public static void stopMockServer() {
        mockRestServer.close();
    }

}