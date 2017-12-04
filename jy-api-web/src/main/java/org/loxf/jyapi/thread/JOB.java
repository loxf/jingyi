package org.loxf.jyapi.thread;

import java.util.concurrent.ExecutorService;

/**
 * Created by luohj on 2017/4/28.
 */
public abstract class JOB {
    protected ExecutorService executor ;
    public abstract void start();

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
}
