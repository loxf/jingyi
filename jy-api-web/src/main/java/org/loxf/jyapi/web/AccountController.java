package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.client.dto.CustBankDto;
import org.loxf.jyadmin.client.service.AccountDetailService;
import org.loxf.jyadmin.client.service.AccountService;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class AccountController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountDetailService accountDetailService;

    @ResponseBody
    @RequestMapping("/api/account/init")
    public BaseResult init(HttpServletRequest request){
        String custId = CookieUtil.getCustId(request);
        return accountService.queryBasicInfo(custId);
    }
    @ResponseBody
    @RequestMapping("/api/account/cashList")
    public BaseResult cashList(int page, int size){
        return new BaseResult();
    }

    /**
     * @param page
     * @param size
     * @param type 1 收入 3 支出
     * @return
     */
    @ResponseBody
    @RequestMapping("/api/account/Detail")
    public BaseResult accountDetail(int page, int size, int type){
        return new BaseResult();
    }
    @ResponseBody
    @RequestMapping("/api/account/bindBankcard")
    public BaseResult bindBankcard(CustBankDto dto){
        return new BaseResult();
    }
    @ResponseBody
    @RequestMapping("/api/account/setPayPassword")
    public BaseResult setPayPassword(String email, String phone, int isChinese, String password, String verifyCode){
        return new BaseResult();
    }
}