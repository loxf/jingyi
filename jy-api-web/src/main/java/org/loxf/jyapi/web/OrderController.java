package org.loxf.jyapi.web;

import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.client.dto.OrderDto;
import org.loxf.jyadmin.client.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     * @param objId 必填，购买对象（商品ID，活动ID）
     * @param orderName 必填，订单名称（这儿直接用商品名称和活动名称，如果是购买会员，就写升级VIP或升级SVIP）
     * @param orderType 必填，1 商品 3 VIP 5 活动
     * @param payType 必填，支付方式 1：微信支付 3：余额支付
     * @return
     */
    @RequestMapping("/api/createOrder")
    @ResponseBody
    public BaseResult createOrder(String objId, String orderName, String orderType, String payType){
        return new BaseResult();
    }
}
