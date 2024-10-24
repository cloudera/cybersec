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

package com.cloudera.cyber.libs.networking;

import com.cloudera.cyber.CyberFunction;
import com.cloudera.cyber.libs.AbstractBooleanScalarFunction;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.net.util.SubnetUtils;

@CyberFunction("in_subnet")
public class InSubnet extends AbstractBooleanScalarFunction {

    private LoadingCache<String, SubnetUtils> cache = Caffeine.newBuilder()
          .expireAfterAccess(60, TimeUnit.MINUTES)
          .maximumSize(1000)
          .build(new CacheLoader<String, SubnetUtils>() {
              @CheckForNull
              @Override
              public SubnetUtils load(@Nonnull String key) throws Exception {
                  return new SubnetUtils(key);
              }
          });

    public Boolean eval(String ip, String subnet) {
        return cache.get(subnet).getInfo().isInRange(ip);
    }

}
