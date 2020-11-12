package com.cloudera.cyber.enrichment.rest;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class NonSecurePostRestRequestTest extends PostRestRequestTest {

    @BeforeClass
    public static void createMockService() {
        createMockService(false);
    }

    @AfterClass
    public static void stopMockServer() {
        mockRestServer.close();
    }

}
