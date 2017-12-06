package org.loxf.jyapi.thread;

import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.weixin.WeixinUtil;
import org.loxf.jyadmin.base.util.weixin.bean.AccessToken;
import org.loxf.jyadmin.base.util.weixin.bean.JsTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Timer;

/**
 * 获取access_token
 */
public class WxAccessTokenFreshJob extends JOB {
    private static Logger logger = LoggerFactory.getLogger(WxAccessTokenFreshJob.class);
    @Autowired
    private JedisUtil jedisUtil;
    private int expireLockMSecd = 60000;
    private int lockTimeout;
    private int period;
    private static String LOCK = "LOCK-WxAccessTokenFreshJob";

    /**
     * @param expireLockMSecd 锁失效时间
     * @param lockTimeout 获取锁的等待时间
     * @param period 业务执行间隔时间
     */
    public WxAccessTokenFreshJob(int expireLockMSecd, int lockTimeout, int period) {
        this.expireLockMSecd = expireLockMSecd;
        this.lockTimeout = lockTimeout;
        this.period = period;
    }

    public void start(){
        Timer timer = new Timer();
        timer.schedule(new Task(LOCK, expireLockMSecd, lockTimeout, new Runnable() {
            @Override
            public void run() {
                deal(jedisUtil);
            }
        }), 30000l, period);
    }

    public static void deal(JedisUtil jedisUtil){
        // 获取redis中的access_token
        String token = jedisUtil.get(BaseConstant.WX_ACCESS_TOKEN);
        AccessToken accessToken = null;
        JsTicket jsTicket = null;
        if(StringUtils.isBlank(token)){
            // 不存在accessToken直接获取刷新
            accessToken = WeixinUtil.queryAccessToken();
            jsTicket = WeixinUtil.queryJsTicket(accessToken.getAccess_token());
        } else {
            String expireTime = jedisUtil.get(token);
            if(StringUtils.isBlank(expireTime)){
                accessToken = WeixinUtil.queryAccessToken();
                jsTicket = WeixinUtil.queryJsTicket(accessToken.getAccess_token());
            }else if(Long.parseLong(expireTime)-System.currentTimeMillis()<10*60*1000){// 最后十分钟 刷新access_token
                accessToken = WeixinUtil.queryAccessToken();
                jsTicket = WeixinUtil.queryJsTicket(accessToken.getAccess_token());
            }
        }
        if(accessToken!=null){
            // 刷新
            long expireTime = System.currentTimeMillis() + accessToken.getExpires_in()*1000;
            jedisUtil.set(BaseConstant.WX_ACCESS_TOKEN, accessToken.getAccess_token(), accessToken.getExpires_in());
            jedisUtil.set(BaseConstant.WX_JS_TICKET, jsTicket.getTicket(), jsTicket.getExpires_in());
            jedisUtil.set(accessToken.getAccess_token(), expireTime+"");
        }
    }

}
