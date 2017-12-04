package org.loxf.jyapi.thread.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WxAccessTokenFreshExe extends Execute {
    private static Logger logger = LoggerFactory.getLogger(WxAccessTokenFreshExe.class);

    public WxAccessTokenFreshExe(int total, int mod) {
        super(total, mod);
    }

    @Override
    public String call() throws Exception {
        logger.debug("执行刷新access_token的逻辑...");
        try {

        } catch (Exception e){
            logger.error("超时订单处理失败：", e);
        }
        return "SUCCESS";
    }
}
