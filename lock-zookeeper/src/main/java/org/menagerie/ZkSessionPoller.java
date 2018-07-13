package org.menagerie;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Mechanism for automatically checking and firing Session Expiration events to the application
 * in the event of the client being disconnected from ZooKeeper for longer than the session timeout.
 * <p>
 * This mechanism is in place for when Disconnected events aren't quite enough--you also need to know
 * about Session Expiration events, and you need to know even in the event of total ZooKeeper connection failure.
 * This may include scenarios like Leader-Election as a governing mechanism for running tasks
 *
 * @author Scott Fines
 */
public final class ZkSessionPoller {
    private static final Logger logger = LoggerFactory.getLogger(ZkSessionPoller.class);
    /*Poll interval in milliseconds*/
    private final long pollIntervalMs;
    /*The zookeeper instance to check*/
    private final ZooKeeper zk;
    private final Object disconnectTimeLock = "Lock";
    private Long startDisconnectTime;

    private final ConnectionListener pollListener;

    /*executor to poll*/
    private final ScheduledExecutorService poller = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread t = new Thread(runnable);
            t.setName("menagerie-ZkConnectionPoller");
            return t;
        }
    });

    public ZkSessionPoller(ZooKeeper zk, long pollIntervalMs, ConnectionListener pollListener) {
        this.pollIntervalMs = pollIntervalMs;
        this.zk = zk;
        this.pollListener = pollListener;
    }

    public static void main(String... args) throws Exception {

        final ZooKeeper zk = new ZooKeeper("172.16.84.129:2181", 2000, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.printf("state=%s\ttype=%s%n", event.getState(), event.getType());
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);

        ZkSessionPoller poller = new ZkSessionPoller(zk, 200, new ConnectionListenerSkeleton() {

            @Override
            public void expired() {
                System.err.println("Session Expired, shutting down.");
                try {
                    zk.close();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                latch.countDown();
            }
        });

        poller.startPolling();
        latch.await();
        poller.stopPolling();
    }

    public void startPolling() {
        poller.scheduleWithFixedDelay(new SessionPoller(), 0l, pollIntervalMs, TimeUnit.MILLISECONDS);
    }

    public void stopPolling() {
        poller.shutdownNow();
    }

    private void expire() {
        //session expired!
        logger.info("Session has expired, notifying listenerand shutting down poller");
        ZkSessionPoller.this.stopPolling();
        pollListener.expired();
    }

    private class SessionPoller implements Runnable {
        private final int sessionTimeoutPeriod;

        private SessionPoller() {
            sessionTimeoutPeriod = zk.getSessionTimeout();
        }

        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) return; //we've been canceled, so return
            if (logger.isTraceEnabled())
                logger.trace("current state of ZooKeeper object: " + zk.getState());
            try {
                zk.exists("/", false);
                synchronized (disconnectTimeLock) {
                    startDisconnectTime = null;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (KeeperException e) {
                if (e.code() == KeeperException.Code.SESSIONEXPIRED) {
                    expire();
                } else if (e.code() == KeeperException.Code.CONNECTIONLOSS) {
                    logger.debug("Received a ConnectionLoss Exception, determining if our session has expired");
                    long currentTime = System.currentTimeMillis();
                    boolean shouldExpire = false;
                    synchronized (disconnectTimeLock) {
                        if (startDisconnectTime == null) {
                            startDisconnectTime = currentTime;
                        } else if ((currentTime - startDisconnectTime) > sessionTimeoutPeriod) {
                            shouldExpire = true;
                        }
                    }
                    if (shouldExpire) expire();
                } else {
                    e.printStackTrace();
                }
            }
        }
    }
}
