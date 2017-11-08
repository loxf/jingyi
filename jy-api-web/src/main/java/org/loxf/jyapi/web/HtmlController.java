package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HtmlController {

    /**
     * 获取富文本内容
     * @param htmlId 富文本ID
     * @return
     */
    @RequestMapping("/api/html/getHtml")
    @ResponseBody
    public BaseResult getHtml(String htmlId){
        return new BaseResult();
    }
}
