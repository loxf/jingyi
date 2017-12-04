package org.loxf.jyapi.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JOBStart {
    private static Logger logger = LoggerFactory.getLogger(JOBStart.class);
    private List<JOB> jobList;
    public JOBStart(List<JOB> jobList){
        this.jobList = jobList;
    }

    public void init(){
        logger.debug("JOB准备启动中...");
        start(Executors.newFixedThreadPool(10));
        logger.debug("JOB启动完成...");
    }

    public void start(ExecutorService executeService){
        if(jobList!=null && jobList.size()>0){
            for(JOB job : jobList){
                job.setExecutor(executeService);
                job.start();
            }
        }
    }
}
