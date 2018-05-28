package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyadmin.client.service.CustService;
import org.loxf.jyadmin.client.service.ShareService;
import org.loxf.jyapi.util.CookieUtil;
import org.loxf.jyapi.util.RequestPayloadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ShareController {
    @Autowired
    private ShareService shareService;
    @Autowired
    private CustService custService;
    @Autowired
    private JedisUtil jedisUtil;
    /**
     * 分享获积分(XCX)
     * @return
     */
    @RequestMapping(value = "/api/share/shareInfoXcx", method = RequestMethod.POST)
    @ResponseBody
    public BaseResult shareInfoXcx(HttpServletRequest request){
        String paramStr = RequestPayloadUtil.getRequestPayload(request);
        JSONObject paramJson = JSON.parseObject(paramStr);
        String openid = paramJson.getString("openid");
        String token = paramJson.getString("token");
        String unionid = jedisUtil.get(openid + CookieUtil.TOKEN_SPLIT + token);
        CustDto custDto = custService.queryCustByUnionId(unionid).getData();
        if(custDto==null){
            return new BaseResult(BaseConstant.FAILED, "客户不存在");
        }
        String detailName = paramJson.getString("detailName");
        String shareObj = paramJson.getString("shareObj");
        String type = paramJson.getString("type");
        return shareService.shareInfo(custDto.getCustId(), detailName, shareObj, type);
    }
    /**
     * 分享获积分
     * @param shareObj 商品/活动ID/url
     * @param type	分享类型：VIDEO/ACTIVE/RECOMMEND/PAGE
     * @return
     */
    @RequestMapping("/api/share/shareInfo")
    @ResponseBody
    public BaseResult shareInfo(HttpServletRequest request, String detailName, String shareObj, String type){
        String custId = CookieUtil.getCustId(request);
        return shareService.shareInfo(custId, detailName, shareObj, type);
    }
    /**
     * 获取分享图片
     * @return
     */
    @RequestMapping("/api/share/queryQRPic")
    @ResponseBody
    public BaseResult queryQRPic(HttpServletRequest request){
        CustDto cust = CookieUtil.getCust(request);
        return shareService.createQR(cust.getNickName(), cust.getCustId());
    }
}
