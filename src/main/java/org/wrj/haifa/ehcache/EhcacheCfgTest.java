package org.wrj.haifa.ehcache;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.Configuration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.xml.XmlConfiguration;

import java.net.URL;

/**
 * Created by wangrenjun on 2017/4/20.
 */
public class EhcacheCfgTest {

    public static void main(String[] args) {
        URL url = EhcacheCfgTest.class.getResource("/ehcache.xml");
        Configuration cfg = new XmlConfiguration(url);
        CacheManager cacheManager = CacheManagerBuilder.newCacheManager(cfg);

        Cache<String, String> cache = cacheManager.createCache("basicCache",
                                 CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
                                                                                        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100,
                                                                                                                                            MemoryUnit.MB)));

        for (int i = 0; i < 20; i++) {
            cache.put("key_" + i, "value_" + i);
        }

    }
}
