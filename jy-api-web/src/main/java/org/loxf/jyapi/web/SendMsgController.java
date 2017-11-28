package org.loxf.jyapi.web;

import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyadmin.client.service.VerifyCodeService;
import org.loxf.jyapi.util.ConfigUtil;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SendMsgController {
    @Autowired
    private VerifyCodeService verifyCodeService;
    /**
     * @param obj
     * @param type 1：设置支付密码， 2：绑定用户
     * @return
     */
    @ResponseBody
    @RequestMapping("/api/sendMsg")
    public BaseResult sendMsg(HttpServletRequest request, String obj, int type){
        CustDto cust = CookieUtil.getCust(request);
        if(StringUtils.isBlank(obj)){
            Integer isChinese = cust.getIsChinese();
            if(isChinese==null){
                return new BaseResult(BaseConstant.FAILED, "联系方式为空");
            }
            if(isChinese==1){
                obj = cust.getPhone();
            } else if(isChinese==2){
                obj = cust.getEmail();
            }
        }
        if(type!=1 && type!=2){
            return new BaseResult(BaseConstant.FAILED, "短信类型为空");
        }
        return verifyCodeService.sendVerifyCode(cust.getCustId(), obj, type);
    }
}
