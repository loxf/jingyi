package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ShareController {
    /**
     * 分享获积分
     * @param shareObj 商品/活动ID/url
     * @param type	商品/活动/page
     * @return
     */
    @RequestMapping("/api/share/shareInfo")
    @ResponseBody
    public BaseResult offerDetail(String shareObj, String type){
        return new BaseResult();
    }
    /**
     * 看视频获得积分
     * @param minute 观看分钟
     * @param videoId	视频ID
     * @return
     */
    @RequestMapping("/api/share/offerDetail")
    @ResponseBody
    public BaseResult offerDetail(Integer minute, String videoId){
        return new BaseResult();
    }
}
