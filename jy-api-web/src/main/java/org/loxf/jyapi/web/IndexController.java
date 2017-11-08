package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IndexController {

    @RequestMapping("/api/index/init")
    @ResponseBody
    public BaseResult initIndex(){
        return new BaseResult();
    }

    /**
     * 签到时间，当前为0
     * @param signType
     * @return
     */
    @RequestMapping("/api/sign")
    @ResponseBody
    public BaseResult initIndex(Integer signType){
        return new BaseResult();
    }

    /**
     * @return
     */
    @RequestMapping("/api/cust/beAgent")
    @ResponseBody
    public BaseResult beAgent( String province, String city, String realName, String phone, Integer type){
        return new BaseResult();
    }

    /**
     * @return
     */
    @RequestMapping("/api/active/list")
    @ResponseBody
    public BaseResult activeList(Integer page, Integer size){
        return new BaseResult();
    }

    /**
     * 课程分类接口
     * @return
     */
    @RequestMapping("/api/offer/catalog")
    @ResponseBody
    public BaseResult offerCatalogList(int size){
        return new BaseResult();
    }
}
