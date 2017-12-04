package org.loxf.jyapi.thread;

import org.loxf.jyadmin.base.util.IdGenerator;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.SpringApplicationContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

/**
 * Created by luohj on 2017/4/28.
 */
public class Task extends TimerTask {
    private Logger logger = LoggerFactory.getLogger(Task.class);
    private JedisUtil jedisUtil;
    private String LOCK;// 锁名
    private int expireLockMSecd = 60000;// 锁失效时间
    private int lockTimeout;//获取锁等待timeout的时间
    private Runnable runnable;// 执行逻辑
    private String name;

    /**
     * @param lock 锁名
     * @param expireLockMSecd 锁失效时间
     * @param lockTimeout 获取锁等待timeout的时间
     * @param runnable 执行逻辑
     */
    public Task(String lock, int expireLockMSecd, int lockTimeout, Runnable runnable) {
        this.jedisUtil = SpringApplicationContextUtil.getBean(JedisUtil.class);
        this.LOCK = lock;
        this.expireLockMSecd = expireLockMSecd;
        this.lockTimeout = lockTimeout;
        this.runnable = runnable;

        this.name = IdGenerator.generate("JOB-");
        logger.debug(LOCK + "[" + name + "]" + "成功启动---------------------");
    }

    public void run() {
        logger.debug(LOCK + "[" + name + "]" + "运行---------------------");
        jedisUtil.lock(LOCK, name, expireLockMSecd, lockTimeout, runnable);
        logger.debug(LOCK + "[" + name + "]" + "运行完---------------------");
    }
}
