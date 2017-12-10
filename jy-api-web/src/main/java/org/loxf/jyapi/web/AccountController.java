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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    @RequestMapping("/api/account/balance")
    public BaseResult getBalance(HttpServletRequest request) {
        String custId = CookieUtil.getCustId(request);
        BaseResult<BigDecimal> basicInfo = accountService.queryBalance(custId);
        return basicInfo;
    }

    @ResponseBody
    @RequestMapping("/api/account/init")
    public BaseResult init(HttpServletRequest request) {
        CustDto custDto = CookieUtil.getCust(request);
        BaseResult<JSONObject> basicInfo = accountService.queryBasicInfo(custDto.getCustId());
        if(basicInfo.getCode()==BaseConstant.SUCCESS) {
            basicInfo.getData().put("isBindUser", custDto.getIsChinese() != null &&
                    (StringUtils.isNotBlank(custDto.getPhone()) || StringUtils.isNotBlank(custDto.getEmail())));
        }
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
        if (StringUtils.isBlank(dto.getBankNo()) || StringUtils.isBlank(dto.getBankNo()) || StringUtils.isBlank(dto.getPhone())
                || StringUtils.isBlank(dto.getUserName())) {
            return new BaseResult(BaseConstant.FAILED, "参数不全");
        }
        dto.setCustId(custId);
        return custBankService.addBankCard(dto);
    }

    @ResponseBody
    @RequestMapping("/api/account/unbindBankcard")
    public BaseResult bindBankcard(HttpServletRequest request, String cardId) {
        if (StringUtils.isBlank(cardId )) {
            return new BaseResult(BaseConstant.FAILED, "参数为空");
        }
        return custBankService.unBind(cardId);
    }

    @ResponseBody
    @RequestMapping("/api/account/cardList")
    public PageResult<CustBankDto> cardList(HttpServletRequest request, Integer page, Integer size) {
        String custId = CookieUtil.getCustId(request);
        CustBankDto custBankDto = new CustBankDto();
        custBankDto.setCustId(custId);
        custBankDto.setStatus(1);
        custBankDto.setPager(new Pager(page, size));
        return custBankService.pager(custBankDto);
    }

    @ResponseBody
    @RequestMapping("/api/account/bankList")
    public BaseResult<List<Map<String, String>>> bankList() {
        return custBankService.queryBankList();
    }

    @ResponseBody
    @RequestMapping("/api/account/setPayPassword")
    public BaseResult setPayPassword(HttpServletRequest request, String password, String verifyCode) {
        CustDto cust = CookieUtil.getCust(request);
        BaseResult baseResult1 = accountService.setPayPassword(cust.getCustId(), cust.getEmail(),
                cust.getPhone(), cust.getIsChinese(), password, verifyCode);
        return baseResult1;
    }

    @RequestMapping("/api/account/getCash")
    @ResponseBody
    public BaseResult getCash(HttpServletRequest request, CustCashDto custCashDto, String password, String sign){
        String custId = CookieUtil.getCustId(request);
        custCashDto.setCustId(custId);
        return custCashService.addCustCashRecord(custCashDto, password, sign);
    }
}
