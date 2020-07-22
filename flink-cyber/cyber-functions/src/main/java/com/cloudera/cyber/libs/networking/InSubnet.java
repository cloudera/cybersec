package com.cloudera.cyber.libs.networking;

import com.cloudera.cyber.CyberFunction;
import com.cloudera.cyber.libs.AbstractBooleanScalarFunction;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.net.util.SubnetUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

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
