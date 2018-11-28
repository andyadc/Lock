package com.andyadc.lock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <URL>http://wudashan.com/2017/10/23/Redis-Distributed-Lock-Implement/</URL>
 * <p>只考虑Redis服务端单机部署的场景</p>
 *
 * @author andaicheng
 * @since 2018/4/22
 */
public class SimpleRedisLock {

    private static final String LOCK_SUCCESS = "OK";
    private static final Long RELEASE_SUCCESS = 1L;
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";

    private static final String LOCK_KEY_PREFIX = "lock:";
    private static final String LOCK_VALUE_PREFIX = "lock:v:";

    private final UUID uuid = UUID.randomUUID();
    private String lockValue;

    /**
     * A local mutex lock for managing inter-thread synchronization
     */
    private final ReentrantLock localLock = new ReentrantLock(false);

    private JedisPool jedisPool;

    public SimpleRedisLock(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.lockValue = this.getLockValue();
    }

    /**
     * Acquires the lock only if it is free at the time of invocation.
     */
    public boolean tryLock(String lockKey,
                           int expireTime) {
        lockKey = LOCK_KEY_PREFIX + lockKey;
        try (Jedis jedis = jedisPool.getResource()) {
            return this.tryLockInner(jedis, lockKey, expireTime, lockValue);
        }
    }

    public boolean unlock(String lockKey) {
        lockKey = LOCK_KEY_PREFIX + lockKey;
        try (Jedis jedis = jedisPool.getResource()) {
            return releaseLockInner(jedis, lockKey, lockValue);
        }
    }

    /**
     * 生成唯一的 value
     */
    private String getLockValue() {
        return LOCK_VALUE_PREFIX + uuid + ":" + Thread.currentThread().getId();
    }

    private boolean tryLockInner(Jedis jedis,
                                 String lockKey,
                                 int expireTime,
                                 String lockValue) {
        String result = jedis.set(lockKey, lockValue, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
        return LOCK_SUCCESS.equals(result);
    }

    private void lockInner(Jedis jedis,
                           String lockKey,
                           int expireTime,
                           String lockValue) {
        for (; ; ) {
            localLock.lock();
            try {
                String result = jedis.set(lockKey, lockValue, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
                if (LOCK_SUCCESS.equals(result)) {
                    return;
                }
            } finally {
                localLock.unlock();
            }
        }
    }

    private boolean releaseLockInner(Jedis jedis,
                                     String lockKey,
                                     String lockValue) {

        // lua script
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(lockValue));
        return RELEASE_SUCCESS.equals(result);
    }
}
