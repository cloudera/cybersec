/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.stellar.common.shell.cli;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

public class StellarShellOptionsValidator {

  private static final Pattern validPortPattern = Pattern.compile("(^.*)[:](\\d+)$");
  private static final Predicate<String> hostnameValidator = hostname -> {
    if(StringUtils.isEmpty(hostname)) {
      return false;
    }
    try {
      InetAddress add = InetAddress.getByName(hostname);
      return true;
    } catch (UnknownHostException e) {
      return false;
    }
  };



  private static final InetAddressValidator inetAddressValidator = InetAddressValidator
      .getInstance();

  /**
   * Validates Stellar CLI Options.
   */
  public static void validateOptions(CommandLine commandLine) throws IllegalArgumentException {
    if (commandLine.hasOption('z')) {
      validateZookeeperOption(commandLine.getOptionValue('z'));
    }
    // v, irc, p are files
    if (commandLine.hasOption('v')) {
      validateFileOption("v", commandLine.getOptionValue('v'));
    }
    if (commandLine.hasOption("irc")) {
      validateFileOption("irc", commandLine.getOptionValue("irc"));
    }
    if (commandLine.hasOption('p')) {
      validateFileOption("p", commandLine.getOptionValue('p'));
    }

  }

  /**
   * Zookeeper argument should be in the form [HOST|IP]:PORT.
   *
   * @param zMulti the zookeeper url fragment
   */
  private static void validateZookeeperOption(String zMulti) throws IllegalArgumentException {
    for(String z : Splitter.on(",").split(zMulti)) {
      Matcher matcher = validPortPattern.matcher(z);
      boolean hasPort = z.contains(":");
      if (hasPort && !matcher.matches()) {
        throw new IllegalArgumentException(String.format("Zookeeper option must have valid port: %s", z));
      }

      if (hasPort && matcher.groupCount() != 2) {
        throw new IllegalArgumentException(
                String.format("Zookeeper Option must be in the form of [HOST|IP]:PORT  %s", z));
      }
      String name = hasPort?matcher.group(1):z;
      Integer port = hasPort?Integer.parseInt(matcher.group(2)):null;

      if (!hostnameValidator.test(name) && !inetAddressValidator.isValid(name)) {
        throw new IllegalArgumentException(
                String.format("Zookeeper Option %s is not a valid host name or ip address  %s", name, z));
      }

      if (hasPort && (port == 0 || port > 65535)) {
        throw new IllegalArgumentException(
                String.format("Zookeeper Option %s port is not valid", z));
      }
    }
  }

  /**
   * File options must exist and be readable.
   *
   * @param option name of the option
   * @param fileName the file name
   */
  private static void validateFileOption(String option, String fileName)
      throws IllegalArgumentException {
    File file = new File(fileName);
    if (!file.exists()) {
      throw new IllegalArgumentException(
          String.format("%s: File %s doesn't exist", option, fileName));
    }
    if (!file.canRead()) {
      throw new IllegalArgumentException(
          String.format("%s: File %s is not readable", option, fileName));
    }
  }
}


