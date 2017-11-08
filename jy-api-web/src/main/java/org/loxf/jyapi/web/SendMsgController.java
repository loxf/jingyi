package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SendMsgController {
    @ResponseBody
    @RequestMapping("/api/sendMsg")
    public BaseResult setPayPassword(String obj, int type){
        return new BaseResult();
    }
}
