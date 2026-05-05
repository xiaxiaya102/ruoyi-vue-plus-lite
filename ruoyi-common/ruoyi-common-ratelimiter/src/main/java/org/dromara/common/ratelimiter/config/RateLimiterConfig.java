package org.dromara.common.ratelimiter.config;

import org.dromara.common.ratelimiter.aspectj.RateLimiterAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author guangxin
 * @date 2023/1/18
 */
@AutoConfiguration
public class RateLimiterConfig {

    @Bean
    public RateLimiterAspect rateLimiterAspect() {
        return new RateLimiterAspect();
    }

}
