package com.cloudera.cyber.enrichment.cidr;

public class IpRegionCidrTestData {
    public static final String IPV4_10_ADDRESS = "10.1.1.2";
    public static final String IPV4_10_SHORT_ADDRESS = "10.1.8.2";
    public static final String IPV4_15_ADDRESS = "15.1.1.3";
    public static final String IPV6_10_ADDRESS = "::ffff:a01:203";
    public static final String IPV6_15_ADDRESS = "::ffff:f01:103";
    public static final String IPV4_ADDRESS_OUT_RANGE = "1.3.4.5";
    public static final String IPV4_MASK_REGION_1 = "10.1.0.0/16";
    public static final String IPV4_SHORT_MASK_REGION = "10.1.8.0/24";
    public static final String IPV6_MASK_REGION_2 = "::ffff:f01:0/112";
    public static final String IPV4_MASK_REGION_1_NAME = "test1";
    public static final String IPV6_MASK_REGION_2_NAME = "test2";
    public static final String IPV4_SHORT_MASK_REGION_NAME = "short_name";

    public static final String UNKNOWN_HOST_IP = "this.is.not.ip";
}
