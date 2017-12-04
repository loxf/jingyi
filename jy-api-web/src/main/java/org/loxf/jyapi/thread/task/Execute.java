package org.loxf.jyapi.thread.task;

import java.util.concurrent.Callable;

/**
 * Created by luohj on 2017/4/28.
 */
public abstract class Execute implements Callable<String> {
    public abstract String call() throws Exception ;
    int total;
    int mod;

    public Execute(int total, int mod){
        this.total = total;
        this.mod = mod;
    }
}
