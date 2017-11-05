package org.loxf.jyapi.util;

import redis.clients.jedis.Jedis;

/**
 * Created by luohj on 2017/4/20.
 */
public class JedisLock {
    private Jedis jedis;
    private String lockKey;
    private static long expireMsecs = 1000 * 60 * 5; // min 锁持有超时 防止线程在入锁以后，无限的执行下去，让锁无法释放
    private long expires = 0l;
    private long timeoutMsecd = 60000;//默认一分钟超时时间
    /**
     * @param jedis
     * @param lockKey 锁名
     */
    protected JedisLock(Jedis jedis, String lockKey) {
        this.jedis = jedis;
        this.lockKey = lockKey;
    }
    /**
     * @param jedis
     * @param lockKey 锁名
     * @param timeoutMsecd 获取锁超时时间 毫秒
     */
    protected JedisLock(Jedis jedis, String lockKey, long timeoutMsecd) {
        this.jedis = jedis;
        this.lockKey = lockKey;
        this.timeoutMsecd = timeoutMsecd;
    }

    /**
     * 加锁
     * @param expireMSeconds 毫秒
     * @return
     */
    protected synchronized boolean acquire(long expireMSeconds) {
        long timeout = timeoutMsecd;
        while (timeout >= 0) {
            expires = System.currentTimeMillis();
            if (expireMSeconds > 0) {
                expires = expires + expireMSeconds + 1;
            } else {
                expires = expires + expireMsecs + 1;
            }
            String expiresStr = String.valueOf(expires);//锁到期时间
            if (jedis.setnx(lockKey, expiresStr) == 1) {
                return true;
            }
            String currentValueStr = jedis.get(lockKey); //redis里的时间
            // 表示已经锁失效，要重新设置锁
            if (currentValueStr != null && Long.parseLong(currentValueStr) < System.currentTimeMillis()) {
                //判断是否为空，不为空的情况下，如果被其他线程设置了值，则下面的条件判断是过不去的
                // lock is expired
                String oldValueStr = jedis.getSet(lockKey, expiresStr);
                //获取上一个锁到期时间，并设置现在的锁到期时间，
                //只有一个线程才能获取上一个线上的设置时间，因为jedis.getSet是同步的
                if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
                    //如过这个时候，多个线程恰好都到了这里，但是只有一个线程的设置值和当前值相同，他才有权利获取锁
                    return true;
                }
            }
            timeout -= 500;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 释放锁
     */
    protected synchronized void release() {
        String currentValueStr = jedis.get(lockKey);
        if (currentValueStr != null && Long.parseLong(currentValueStr) > System.currentTimeMillis() && Long.valueOf(currentValueStr) == expires) {
            // 锁未失效且是当前线程获取的锁才能删
            jedis.del(lockKey);
        }
    }
}
