package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.bean.PageResult;
import org.loxf.jyadmin.base.bean.Pager;
import org.loxf.jyadmin.client.dto.AccountDetailDto;
import org.loxf.jyadmin.client.dto.CustBankDto;
import org.loxf.jyadmin.client.dto.CustCashDto;
import org.loxf.jyadmin.client.service.AccountDetailService;
import org.loxf.jyadmin.client.service.AccountService;
import org.loxf.jyadmin.client.service.CustCashService;
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
    @Autowired
    private CustCashService custCashService;

    @ResponseBody
    @RequestMapping("/api/account/init")
    public BaseResult init(HttpServletRequest request){
        String custId = CookieUtil.getCustId(request);
        return accountService.queryBasicInfo(custId);
    }
    @ResponseBody
    @RequestMapping("/api/account/cashList")
    public PageResult cashList(HttpServletRequest request, Integer page, Integer size){
        String custId = CookieUtil.getCustId(request);
        if(page==null || page<=0){
            page = 1;
        }
        if(size==null || size<0){
            size = 10;
        }
        CustCashDto dto = new CustCashDto();
        dto.setPager(new Pager(page, size));
        dto.setCustId(custId);
        PageResult<CustCashDto> pageResult = custCashService.queryCustCash(dto);
        return pageResult;
    }

    /**
     * @param page
     * @param size
     * @param type 1 收入 3 支出
     * @return
     */
    @ResponseBody
    @RequestMapping("/api/account/Detail")
    public PageResult accountDetail(HttpServletRequest request, Integer page, Integer size, Integer type){
        String custId = CookieUtil.getCustId(request);
        if(page==null || page<=0){
            page = 1;
        }
        if(size==null || size<=0){
            size = 10;
        }
        AccountDetailDto accountDetailDto = new AccountDetailDto();
        accountDetailDto.setCustId(custId);
        accountDetailDto.setType(type);
        accountDetailDto.setPager(new Pager(page, size));
        return accountDetailService.queryDetails(accountDetailDto);
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
