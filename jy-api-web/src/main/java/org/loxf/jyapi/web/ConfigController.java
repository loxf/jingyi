package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.base.util.weixin.WeixinUtil;
import org.loxf.jyadmin.client.service.ConfigService;
import org.loxf.jyadmin.client.service.ProvinceAndCityService;
import org.loxf.jyapi.thread.WxAccessTokenFreshJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class ConfigController {
    @Autowired
    private ConfigService configService;
    @Autowired
    private ProvinceAndCityService provinceAndCityService;
    @Autowired
    private JedisUtil jedisUtil;

    @RequestMapping("/api/system/wxUrlConfig")
    @ResponseBody
    public BaseResult getConfig(String url){
        String jsapiTicket = jedisUtil.get(BaseConstant.WX_JS_TICKET);
        if(StringUtils.isBlank(jsapiTicket)){
            WxAccessTokenFreshJob.deal(jedisUtil);
            jsapiTicket = jedisUtil.get(BaseConstant.WX_JS_TICKET);
            if(StringUtils.isBlank(jsapiTicket)){
                return new BaseResult(BaseConstant.FAILED, "获取TOKEN失败");
            }
        }
        Map info = WeixinUtil.signJsTicket(jsapiTicket, url);
        return new BaseResult(info);
    }

    @RequestMapping("/api/system/getConfig")
    @ResponseBody
    public BaseResult getConfig(String catalog, String configCode){
        return configService.queryConfig(catalog, configCode);
    }

    @RequestMapping("/api/system/getArea")
    @ResponseBody
    public BaseResult getArea(Integer type){
        return provinceAndCityService.queryAreaByTree(type);
    }
}
