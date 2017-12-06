package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.client.dto.*;
import org.loxf.jyadmin.client.service.ActiveService;
import org.loxf.jyadmin.client.service.OfferService;
import org.loxf.jyadmin.client.service.OrderService;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OfferService offerService;
    @Autowired
    private ActiveService activeService;

    /**
     * 创建订单
     * @param objId 必填，购买对象（商品ID，活动ID）
     * @param orderType 必填，1 商品 3 VIP 5 活动
     * @param payType 必填，支付方式 1：微信支付 3：余额支付
     * @return
     */
    @RequestMapping("/api/createOrder")
    @ResponseBody
    public BaseResult createOrder(HttpServletRequest request, String objId, Integer orderType, Integer payType, List<OrderAttrDto> attrList){
        if (StringUtils.isBlank(objId) || orderType == null
                || payType == null) {
            return new BaseResult(BaseConstant.FAILED, "关键参数缺失");
        }
        CustDto custDto = CookieUtil.getCust(request);
        String lv = custDto.getUserLevel();
        // 订单
        OrderDto orderDto = new OrderDto();
        String privi = "";
        BaseResult<Boolean> hasBuyOrder = orderService.hasBuy(custDto.getCustId(), orderType, objId);
        if(hasBuyOrder.getCode()==BaseConstant.FAILED){
            return new BaseResult(BaseConstant.FAILED, hasBuyOrder.getMsg());
        }
        if(hasBuyOrder.getData()){
            return new BaseResult(BaseConstant.FAILED, "当前商品已经购买过");
        }
        if(orderType.intValue()==1){
            OfferDto offerDto = offerService.queryOffer(objId).getData();
            if(offerDto==null){
                return new BaseResult(BaseConstant.FAILED, "商品不存在");
            }
            orderDto.setOrderName(offerDto.getOfferName());
            privi = offerDto.getBuyPrivi();

        } else if(orderType.intValue()==3){
            OfferDto offerDto = offerService.queryOffer(objId).getData();
            if(offerDto==null){
                return new BaseResult(BaseConstant.FAILED, "服务不存在");
            }
            // 服务价格单独处理
            if(lv.equals("VIP") && offerDto.getOfferName().equals("SVIP")){
                // vip 升级 svip
                orderDto.setOrderMoney(new BigDecimal("400"));
                orderDto.setTotalMoney(new BigDecimal("400"));
            } else {
                orderDto.setOrderMoney(offerDto.getSaleMoney());
                orderDto.setTotalMoney(offerDto.getSaleMoney());
            }
            orderDto.setOrderName("升级" + offerDto.getOfferName());
        } else if(orderType.intValue()==5){
            ActiveDto activeDto = activeService.queryActive(objId).getData();
            if(activeDto==null){
                return new BaseResult(BaseConstant.FAILED, "活动不存在");
            }
            orderDto.setOrderName(activeDto.getActiveName());
            privi = activeDto.getActivePrivi();
        }
        if(orderType.intValue()!=3) {
            if (StringUtils.isBlank(privi)) {
                return new BaseResult(BaseConstant.FAILED, "当前商品不能购买");
            } else {
                JSONObject priviJson = JSONObject.parseObject(privi);
                if (!priviJson.containsKey(lv)) {
                    return new BaseResult(BaseConstant.FAILED, "无权购买当前商品");
                } else {
                    String price = priviJson.getString(lv);
                    orderDto.setOrderMoney(new BigDecimal(price));
                    orderDto.setTotalMoney(new BigDecimal(price));
                }
            }
        }

        orderDto.setObjId(objId);
        orderDto.setOrderType(orderType);
        orderDto.setPayType(payType);
        orderDto.setCustId(custDto.getCustId());
        orderDto.setBp(BigDecimal.ZERO);
        orderDto.setDiscount(10L);
        return orderService.createOrder(orderDto, attrList);
    }
}
