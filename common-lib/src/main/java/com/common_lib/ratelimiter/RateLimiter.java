package com.common_lib.ratelimiter;

public interface RateLimiter {

    RateLimitResult check(String key, int maxRequestAllowed, long retryAfterSeconds);

}
