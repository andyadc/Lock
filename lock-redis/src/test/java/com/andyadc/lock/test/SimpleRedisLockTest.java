package com.andyadc.lock.test;

import com.andyadc.lock.SimpleRedisLock;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author andaicheng
 * @since 2018/4/22
 */
public class SimpleRedisLockTest {

    private static final String QQ_SERVER = "www.qq-server.com";
    private static final String LOCK_SERVER = "127.0.0.1";
    private static final int REDIS_PORT = 6377;
    private static final AtomicInteger num = new AtomicInteger(0);
    private static final JedisPoolConfig config;
    private static JedisPool jedisPool;

    static {
        config = new JedisPoolConfig();
        config.setMaxTotal(30);
        config.setMaxIdle(10);

        jedisPool = new JedisPool(config, QQ_SERVER, REDIS_PORT, 30000, "andyadc");
    }

    @Rule
    public ContiPerfRule rule = new ContiPerfRule();

    @Test
    public void ping() {
        Jedis jedis = jedisPool.getResource();
        System.out.println(jedis.ping());
    }

    @Test
    public void lock() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(20, (runnable) -> new Thread(runnable, "lock-redis-threadpool-"));
        CountDownLatch latch = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) {
            executorService.execute(new Locker(jedisPool, latch));
        }
        executorService.shutdown();

        latch.await();
        System.out.println("-----------------------------------------------");
    }

    //@PerfTest(threads = 10, invocations = 10)
    @Test
    public void lock2() {
        SimpleRedisLock lock = new SimpleRedisLock(jedisPool);
        System.out.println(lock.tryLock("adc", 100000));
    }

    @PerfTest(threads = 10, invocations = 10)
    @Test
    public void unlock() {
        SimpleRedisLock lock = new SimpleRedisLock(jedisPool);
        System.out.println(lock.unlock("adc"));
    }

    static class Locker implements Runnable {

        private JedisPool jedisPool;
        private CountDownLatch latch;

        public Locker(JedisPool jedisPool, CountDownLatch latch) {
            this.jedisPool = jedisPool;
            this.latch = latch;
        }

        @Override
        public void run() {
            SimpleRedisLock lock = new SimpleRedisLock(jedisPool);
            System.out.println(Thread.currentThread().getName() + num.incrementAndGet() + " require lock result: "
                    + lock.tryLock("adc", 100000)
                    + " " + LocalDateTime.now());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
        }
    }
}
