package com.cloudera.cyber.generator;

import com.github.javafaker.Faker;
import org.apache.commons.net.util.SubnetUtils;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

public class RandomGenerators implements Serializable {
    private static final Faker faker = new Faker();

    public static String randomIP() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
    }

    public static String randomIP(String subnet) {
        SubnetUtils utils = new SubnetUtils(subnet);
        String[] hosts = utils.getInfo().getAllAddresses();
        return hosts[ThreadLocalRandom.current().nextInt(hosts.length)];
    }

    public static int randomInt(int from, int to) {
        return ThreadLocalRandom.current().nextInt(from, to + 1);
    }

    public static String randomChoice(String ...options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }

    public static String randomHost() {
        return "www.cloudera.com";
    }

    public static String randomUrlPath() {
        return "/test";
    }

    public static String canonicalize(String host) {
        return host.replaceAll("^([^\\.]*)\\.(.*)$", "$1.extra.$2");
    }

    public static String randomEmail() {
        return faker.internet().emailAddress();
    }
    public static String randomUser() {
        return faker.name().username();
    }

    public static String randomName() {
        return faker.name().firstName() + " " + faker.name().lastName();
    }

    public static String randomSubject() {
        return faker.lorem().sentence();
    }
}
