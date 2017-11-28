package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.bean.PageResult;
import org.loxf.jyadmin.base.bean.Pager;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.client.dto.AccountDetailDto;
import org.loxf.jyadmin.client.dto.CustBankDto;
import org.loxf.jyadmin.client.dto.CustCashDto;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyadmin.client.service.*;
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
    @Autowired
    private CustBankService custBankService;

    @ResponseBody
    @RequestMapping("/api/account/init")
    public BaseResult init(HttpServletRequest request) {
        CustDto custDto = CookieUtil.getCust(request);
        BaseResult<JSONObject> basicInfo = accountService.queryBasicInfo(custDto.getCustId());
        basicInfo.getData().put("isBindUser", custDto.getIsChinese()!=null &&
                (StringUtils.isNotBlank(custDto.getPhone())||StringUtils.isNotBlank(custDto.getEmail())));
        return basicInfo;
    }

    @ResponseBody
    @RequestMapping("/api/account/cashList")
    public PageResult cashList(HttpServletRequest request, Integer page, Integer size) {
        String custId = CookieUtil.getCustId(request);
        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size < 0) {
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
    public PageResult accountDetail(HttpServletRequest request, Integer page, Integer size, Integer type) {
        String custId = CookieUtil.getCustId(request);
        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size <= 0) {
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
    public BaseResult bindBankcard(HttpServletRequest request, CustBankDto dto) {
        if (dto == null) {
            return new BaseResult(BaseConstant.FAILED, "参数为空");
        }
        String custId = CookieUtil.getCustId(request);
        if (StringUtils.isBlank(dto.getBank()) || StringUtils.isBlank(dto.getBankNo()) || StringUtils.isBlank(dto.getPhone())
                || StringUtils.isBlank(dto.getUserName())) {
            return new BaseResult(BaseConstant.FAILED, "参数不全");
        }
        dto.setCustId(custId);
        return custBankService.addBankCard(dto);
    }

    @ResponseBody
    @RequestMapping("/api/account/setPayPassword")
    public BaseResult setPayPassword(HttpServletRequest request, String password, String verifyCode) {
        CustDto cust = CookieUtil.getCust(request);
        BaseResult baseResult1 = accountService.setPayPassword(cust.getCustId(), cust.getEmail(),
                cust.getPhone(), cust.getIsChinese(), password, verifyCode);
        return baseResult1;
    }
}
