package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.exception.BizException;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.weixin.WeixinUtil;
import org.loxf.jyadmin.base.util.weixin.bean.UserAccessToken;
import org.loxf.jyadmin.base.util.weixin.bean.WXUserInfo;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyadmin.client.service.CustService;
import org.loxf.jyapi.util.CookieUtil;
import org.loxf.jyapi.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Random;

@Controller
public class LoginController {
    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private JedisUtil jedisUtil;
    @Autowired
    private CustService custService;

    @RequestMapping("/api/login")
    public void login(HttpServletRequest request, HttpServletResponse response, String targetUrl) {
        // 获取登录随机code，五分钟失效
        String code = getRandomCharAndNumr(8);
        String loginUrl = WeixinUtil.getLoginUrl(targetUrl, code);
        try {
            if ("JY123456QWE".equals(request.getParameter("XDebug"))) {
                loginUrl = String.format(BaseConstant.LOGIN_URL, URLEncoder.encode(targetUrl, "utf-8")) + "&state=" + code + "&XDebug=IYUTERESGBXVCMSWB";
                if ("L".equals(request.getParameter("LOCATION"))) {
                    loginUrl = loginUrl.replaceAll("https://www.jingyizaixian.com", "http://127.0.0.1:8081");
                }
            }
            jedisUtil.set(code, (System.currentTimeMillis() + 5 * 60 * 1000) + "", 5 * 60);
            response.sendRedirect(loginUrl);
        } catch (IOException e) {
            logger.error("登录失败", e);
            throw new RuntimeException("登录失败", e);
        }
    }

    /**
     * 登录
     *
     * @param targetUrl 跳转URL
     * @param code      微信授权CODE
     * @param state     业务系统自行定义的校验码
     * @return
     */
    @RequestMapping("/api/loginByWx")
    public void login(HttpServletRequest request, HttpServletResponse response, String targetUrl, String code, String state) {
        // 检验用户登录随机码
        if (jedisUtil.exists(state)) {
            String expireTime = jedisUtil.get(state);
            if (System.currentTimeMillis() - Long.parseLong(expireTime) > 0) {
                logger.info("登录校验码失效");
                throw new BizException("登录校验码失效");
            } else {
                // 登录成功
                // 请求用户信息
                UserAccessToken userAccessToken = WeixinUtil.queryUserAccessToken(code);
                // TODO 注释DEBUG
                if ("IYUTERESGBXVCMSWB".equals(request.getParameter("XDebug"))) {
                    userAccessToken = testUserAccessToken();
                }
                if (userAccessToken != null) {
                    // 获取用户登录token成功 拉取用户信息
                    WXUserInfo wxUserInfo = WeixinUtil.queryUserInfo(userAccessToken.getAccess_token(), userAccessToken.getOpenid());
                    // TODO 注释DEBUG
                    if ("IYUTERESGBXVCMSWB".equals(request.getParameter("XDebug"))) {
                        wxUserInfo = testWXUserInfo();
                    }
                    if (wxUserInfo != null) {
                        Map<String, String> paramMap = UrlUtil.URLRequest(targetUrl);
                        CustDto custDto = settingUser(request, response, paramMap.get("recommend"), userAccessToken, wxUserInfo);
                        // TODO system log
                        try {
                            response.sendRedirect(targetUrl);
                        } catch (IOException e) {
                            logger.error("登录后跳转页面失败", e);
                        }
                    }
                }
            }
            jedisUtil.del(state);
        } else {
            logger.info("登录校验码不存在");
            throw new BizException("登录校验码不存在");
        }
    }

    @RequestMapping("/api/getUserInfo")
    @ResponseBody
    public BaseResult<CustDto> getUserInfo(HttpServletRequest request, HttpServletResponse response) {
        return new BaseResult<>(CookieUtil.getCust(request));
    }

    private UserAccessToken testUserAccessToken() {
        UserAccessToken accessToken = new UserAccessToken();
        accessToken.setAccess_token("asglujwlkrntjkfnvuifd233465gdfkjgds");
        accessToken.setExpires_in("7200");
        accessToken.setOpenid("LAKSJDFOI25JSDJF");
        accessToken.setRefresh_token("aslkughjlafshnguiriuhiu4thddfgoudjh");
        accessToken.setScope("snsapi_userinfo");
        return accessToken;
    }

    private WXUserInfo testWXUserInfo() {
        WXUserInfo wxUserInfo = new WXUserInfo();
        wxUserInfo.setCity("广州");
        wxUserInfo.setProvince("广东");
        wxUserInfo.setCountry("中国");
        wxUserInfo.setNickname("FACE-TEST");
        wxUserInfo.setHeadimgurl("http://wx.qlogo.cn/mmopen/vi_32/Q0j4TwGTfTIayV0CQ8xZWGDnUSkfTwSMKX32hIC4Tf6RibiatgYfePby8J6B0ic5iczicPY6KDE4VAnXpGEoibk0n70A/0");
        wxUserInfo.setOpenid("LAKSJDFOI25JSDJF");
        wxUserInfo.setSex(1);
        wxUserInfo.setUnionid("ijdxjnfnjrkt35kjwefs89");
        return wxUserInfo;
    }

    private CustDto settingUser(HttpServletRequest request, HttpServletResponse response, String recommend,
                                UserAccessToken userAccessToken, WXUserInfo wxUserInfo) {
        //处理微信用户信息
        CustDto custDto = new CustDto();
        BeanUtils.copyProperties(wxUserInfo, custDto);
        custDto.setHeadImgUrl(wxUserInfo.getHeadimgurl());
        custDto.setNickName(wxUserInfo.getNickname());
        BaseResult<CustDto> baseResult = custService.queryCustByOpenId(wxUserInfo.getOpenid());
        // 计算token失效时间
        int expireSecond = Integer.valueOf(userAccessToken.getExpires_in());
        userAccessToken.setExpires_in((expireSecond * 1000 + System.currentTimeMillis()) + "");
        if (baseResult.getCode() == BaseConstant.SUCCESS) {
            custService.refreshCustByOpenId(custDto, userAccessToken);
        } else {
            custDto.setRecommend(recommend);
            BaseResult<String> custBaseResult = custService.addCust(custDto, userAccessToken);
            custDto.setCustId(custBaseResult.getData());
        }
        // 获取最新的cust信息
        CustDto custInfo = custService.queryCustByOpenId(wxUserInfo.getOpenid()).getData();
        // 生成 系统TOKEN
        String tmp = CookieUtil.TOKEN_PREFIX + CookieUtil.TOKEN_SPLIT + custInfo.getCustId()
                + CookieUtil.TOKEN_SPLIT + System.currentTimeMillis();
        try {
            String token = CookieUtil.encrypt(tmp);
            // 设置cookie session token
            CookieUtil.setSession(request, BaseConstant.USER_COOKIE_NAME, token);
            jedisUtil.set(token, JSON.toJSONString(custInfo), expireSecond);
            setUserCookie(response, token, custInfo.getCustId());
        } catch (Exception e) {
            logger.error("TOKEN加密失败", e);
        }
        return custInfo;
    }

    private void setUserCookie(HttpServletResponse response, String token, String custId) {
        CookieUtil.setCookie(response, BaseConstant.USER_COOKIE_NAME, token);
        CookieUtil.setCookie(response, BaseConstant.JY_CUST_ID, custId);
    }

    /**
     * 获取随机字母数字组合
     *
     * @param length 字符串长度
     * @return
     */
    private static String getRandomCharAndNumr(Integer length) {
        String str = "";
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            boolean b = random.nextBoolean();
            if (b) { // 字符串
                // int choice = random.nextBoolean() ? 65 : 97; 取得65大写字母还是97小写字母
                str += (char) (65 + random.nextInt(26));// 取得大写字母
            } else { // 数字
                str += String.valueOf(random.nextInt(10));
            }
        }
        return str;
    }
}
