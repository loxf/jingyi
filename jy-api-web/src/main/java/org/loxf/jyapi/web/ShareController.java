package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.client.service.ShareService;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ShareController {
    @Autowired
    private ShareService shareService;
    /**
     * 分享获积分
     * @param shareObj 商品/活动ID/url
     * @param type	分享类型：VIDEO/ACTIVE/RECOMMEND/PAGE
     * @return
     */
    @RequestMapping("/api/share/shareInfo")
    @ResponseBody
    public BaseResult offerDetail(HttpServletRequest request, String detailName, String shareObj, String type){
        String custId = CookieUtil.getCustId(request);
        return shareService.shareInfo(custId, detailName, shareObj, type);
    }
}
