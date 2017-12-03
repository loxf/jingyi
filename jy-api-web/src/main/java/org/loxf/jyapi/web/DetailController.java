package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.client.dto.CustDto;
import org.loxf.jyadmin.client.dto.OfferDto;
import org.loxf.jyadmin.client.dto.PurchasedVideoDto;
import org.loxf.jyadmin.client.dto.VideoConfigDto;
import org.loxf.jyadmin.client.service.ActiveService;
import org.loxf.jyadmin.client.service.OfferService;
import org.loxf.jyadmin.client.service.PurchasedVideoService;
import org.loxf.jyadmin.client.service.VideoConfigService;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class DetailController {
    @Autowired
    private OfferService offerService;
    @Autowired
    private ActiveService activeService;
    @Autowired
    private VideoConfigService videoConfigService;
    @Autowired
    private PurchasedVideoService purchasedVideoService;

    @RequestMapping("/api/offer/detail")
    @ResponseBody
    public BaseResult offerDetail(HttpServletRequest request, String offerId){
        CustDto custDto = CookieUtil.getCust(request);
        BaseResult<OfferDto> offerDtoBaseResult = offerService.queryOffer(offerId);
        if(offerDtoBaseResult.getCode()== BaseConstant.FAILED){
            return offerDtoBaseResult;
        }
        OfferDto offerDto = offerDtoBaseResult.getData();
        if(offerDto==null){
            return new BaseResult(BaseConstant.FAILED, "套餐不存在");
        }
        if(offerDto.getStatus()==0){
            return new BaseResult(BaseConstant.FAILED, "套餐已下架");
        }
        // 判断是套餐还是课程 商品类型 服务类型：VIP,  课程：CLASS, 套餐：OFFER
        String type = offerDto.getOfferType();
        if("OFFER".equals(type)){
            JSONObject result = new JSONObject();
            // 基本信息
            result.put("htmlId", offerDto.getHtmlId());
            result.put("offerName", offerDto.getOfferName());
            // 页面展现按钮
            JSONArray btns = new JSONArray();
            String buyPriviStr = offerDto.getBuyPrivi();
            // 判断视频是否可播放，获取页面展现的按钮 1/用户已购买此套餐，2/此套餐对VIP/SVIP用户免费
            boolean canPlay = dealOfferBtn(custDto.getCustId(), custDto.getUserLevel(), offerId, buyPriviStr, btns);

            result.put("isPlay", (canPlay?1:0));
            result.put("btns", btns);
            BaseResult<List<OfferDto>> relOfferBaseResult = offerService.showOfferRel(offerId, "OFFER");
            if(relOfferBaseResult.getCode()==BaseConstant.FAILED || CollectionUtils.isEmpty(relOfferBaseResult.getData())){
                return new BaseResult(BaseConstant.FAILED, "获取套餐内容失败");
            }
            String mainMedia = null;// 套餐主媒体
            // 获取套餐内容
            JSONArray offerList = new JSONArray();
            List<OfferDto> offerRels = relOfferBaseResult.getData();
            for (OfferDto childOffer : offerRels){
                JSONObject ofrJson = new JSONObject();
                if(!canPlay) {
                    // 当前视频不能播放的时候，单独显示购买按钮
                    String relBuyPriviStr = childOffer.getBuyPrivi();
                    if (StringUtils.isBlank(relBuyPriviStr)) {
                        // 不能单独购买
                        ofrJson.put("canBuy", 0);
                    } else {
                        ofrJson.put("canBuy", 1);
                    }
                } else {
                    // 不展现单独购买按钮
                    ofrJson.put("canBuy", -1);
                    ofrJson.put("canBuy", -1);
                }
                if(childOffer.getOfferType().equals("CLASS")){
                    // 获取乐视视频ID
                    BaseResult<VideoConfigDto> videoConfigDtoBaseResult = videoConfigService.queryVideo(childOffer.getMainMedia());
                    if(videoConfigDtoBaseResult.getCode()==BaseConstant.FAILED || videoConfigDtoBaseResult.getData()==null){
                        return new BaseResult(BaseConstant.FAILED, "获取视频失败");
                    }
                    if(StringUtils.isBlank(mainMedia)){
                        mainMedia = videoConfigDtoBaseResult.getData().getVideoOutId();
                    }
                    ofrJson.put("mainMedia", videoConfigDtoBaseResult.getData().getVideoOutId());
                    String metaDataStr = childOffer.getMetaData();
                    if(StringUtils.isNotBlank(metaDataStr)) {
                        JSONObject metaData = JSON.parseObject(metaDataStr);
                        if (metaData.containsKey("TEACHER")) {
                            ofrJson.put("teacher", metaData.getJSONArray("TEACHER"));
                        } else {
                            ofrJson.put("teacher", null);
                        }
                    } else {
                        ofrJson.put("teacher", null);
                    }
                }
                ofrJson.put("offerId", childOffer.getOfferId());
                ofrJson.put("offerName", childOffer.getOfferName());
                ofrJson.put("offerType", childOffer.getOfferType());
                ofrJson.put("pic", childOffer.getOfferPic());
                ofrJson.put("price", childOffer.getOfferPic());
                offerList.add(ofrJson);
            }
            result.put("mainMedia", mainMedia);
            result.put("offerList", offerList);
            return new BaseResult(result);
        } else {
            return new BaseResult(BaseConstant.FAILED, "当前商品非套餐");
        }
    }
    @RequestMapping("/api/active/detail")
    @ResponseBody
    public BaseResult activeDetail(String activeId){
        return new BaseResult();
    }
    @RequestMapping("/api/offerClass/detail")
    @ResponseBody
    public BaseResult classDetail(String offerId){
        return new BaseResult();
    }

    private JSONObject createBtn(Integer click, String name, String offerId, String price){
        JSONObject buyBtn = new JSONObject();
        buyBtn.put("click", click);
        buyBtn.put("name", name);
        buyBtn.put("offerId", offerId);
        buyBtn.put("price", price);
        return buyBtn;
    }

    private boolean dealOfferBtn(String custId, String lv, String offerId, String buyPriviStr, JSONArray btns){
        boolean canPlay = false ;
        if(StringUtils.isBlank(buyPriviStr)){
            // 不能单独购买
            canPlay = false;
            btns.add(createBtn(0, "套餐不能直接购买", offerId, null));
        } else {
            // 可以购买
            JSONObject buyPrivi = JSON.parseObject(buyPriviStr);
            if(lv.equals("VIP")||lv.equals("SVIP")){
                // 会员
                Object price = buyPrivi.get(lv);
                if(price==null){
                    canPlay = false;
                    if(lv.equals("VIP")){
                        // 查看SVIP是否可以购买
                        Object svipPrice = buyPrivi.get("SVIP");
                        if(svipPrice!=null){
                            btns.add(createBtn(1, "升级SVIP", "OFFER002", "400"));
                        }
                    }
                } else if(Integer.valueOf(price.toString())<=0){
                    // 免费
                    canPlay = true;
                    btns.add(createBtn(1, "分享好友一起学习", offerId, null));
                } else {
                    // 校验是否购买过此套餐
                    if(hasBuy(custId, offerId)){
                        // 已购买
                        canPlay = true;
                        btns.add(createBtn(1, "分享好友一起学习", offerId, null));
                    } else {
                        // 需要购买
                        canPlay = false;
                        btns.add(createBtn(1, "立即购买", offerId, price + ""));
                        if(lv.equals("VIP")){
                            btns.add(createBtn(1, "升级SVIP", "OFFER002", "400"));
                        }
                    }
                }
            } else {
                // 非会员
                Object price = buyPrivi.get(lv);
                if(price==null){
                    canPlay = false;
                    btns.add(createBtn(1, "升级VIP", "OFFER001", "299"));
                    btns.add(createBtn(1, "升级SVIP", "OFFER002", "699"));
                } else if(Integer.valueOf(price.toString())<=0){
                    // 免费
                    canPlay = true;
                    btns.add(createBtn(1, "分享好友一起学习", offerId, null));
                } else {
                    // 校验是否购买过此套餐
                    if(hasBuy(custId, offerId)){
                        // 已购买
                        canPlay = true;
                        btns.add(createBtn(1, "分享好友一起学习", offerId, null));
                    } else {
                        // 需要购买
                        canPlay = false;
                        btns.add(createBtn(1, "立即购买", offerId, price + ""));
                        btns.add(createBtn(1, "升级VIP", "OFFER001", "299"));
                    }
                }
            }
        }
        return canPlay;
    }

    private boolean hasBuy(String custId, String offerId){
        // 校验是否购买过此套餐
        PurchasedVideoDto purchasedVideoDto = new PurchasedVideoDto();
        purchasedVideoDto.setCustId(custId);
        purchasedVideoDto.setOfferId(offerId);
        BaseResult<Integer> hasBuy = purchasedVideoService.count(purchasedVideoDto);
        if(hasBuy.getCode()==BaseConstant.FAILED){
            throw new RuntimeException(hasBuy.getMsg());
        }
        return hasBuy.getData()>0;
    }
}
