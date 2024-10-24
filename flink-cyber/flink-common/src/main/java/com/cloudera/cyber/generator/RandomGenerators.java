/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.generator;

import com.github.javafaker.Faker;
import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.net.util.SubnetUtils;

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

    public static String randomChoice(String... options) {
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
