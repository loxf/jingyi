package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DetailController {
    @RequestMapping("/api/offer/detail")
    @ResponseBody
    public BaseResult offerDetail(String offerId){
        return new BaseResult();
    }
    @RequestMapping("/api/active/detail")
    @ResponseBody
    public BaseResult activeDetail(String activeId){
        return new BaseResult();
    }
    @RequestMapping("/api/offerClass/detail")
    @ResponseBody
    public BaseResult classDetail(String offerId){
        return new BaseResult();
    }
}
