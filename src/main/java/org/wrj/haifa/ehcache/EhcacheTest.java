package org.wrj.haifa.ehcache;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.impl.config.persistence.CacheManagerPersistenceConfiguration;

import java.io.File;

/**
 * Created by wangrenjun on 2017/4/19.
 */
public class EhcacheTest {

    public static void main(String[] args) throws InterruptedException {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured", CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class,
                        String.class, ResourcePoolsBuilder.newResourcePoolsBuilder().disk(200, MemoryUnit.MB)).build())
                .with(new CacheManagerPersistenceConfiguration(new File(".", "myCacheData")))
                .build(true);

        // Cache<Long, String> preConfigured = cacheManager.getCache("preConfigured", Long.class, String.class);

        Cache<Long, String> myCache = cacheManager.createCache("myCache",
                                                               CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class,
                                                                                                                      String.class,
                                                                                                                      // ResourcePoolsBuilder.heap(10)).build());
                                                                                                                      ResourcePoolsBuilder.newResourcePoolsBuilder().offheap(200,
                                                                                                                                                                             MemoryUnit.MB)).build());

        for (long i = 0; i < 20000; i++) {
            myCache.put(i, "value_" + i);
        }

        for (long i = 0; i < 20000; i++) {
            String value = myCache.get(i);
            if (value == null) {
                System.out.println("myCache.get(" + i + ")=" + myCache.get(i));

            }
        }

        Thread.sleep(10000);
        cacheManager.close();
    }

}
