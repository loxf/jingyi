package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.base.util.DateUtils;
import org.loxf.jyadmin.base.util.JedisUtil;
import org.loxf.jyadmin.client.dto.*;
import org.loxf.jyadmin.client.service.*;
import org.loxf.jyapi.util.ConfigUtil;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class DetailController {
    @Autowired
    private CustService custService;
    @Autowired
    private OfferService offerService;
    @Autowired
    private ActiveService activeService;
    @Autowired
    private VideoConfigService videoConfigService;
    @Autowired
    private PurchasedInfoService purchasedInfoService;
    @Autowired
    private ActiveCustListService activeCustListService;
    @Autowired
    private ProvinceAndCityService provinceAndCityService;
    @Autowired
    private JedisUtil jedisUtil;

    private static String CANNOT_BUY = "CANNOT_BUY";
    private static String BUY_NOW = "BUY_NOW";
    private static String SHARE_FRIEND = "SHARE_FRIEND";
    private static String BE_VIP = "BE_VIP";
    private static String BE_SVIP = "BE_SVIP";

    @RequestMapping("/api/offer/detail")
    @ResponseBody
    public BaseResult offerDetail(HttpServletRequest request, String offerId){
        String custId = CookieUtil.getCustId(request);
        CustDto custDto = custService.queryCustByCustId(custId).getData();
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
            String desc = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_COM, "APP_SHARE_DESC",
                    "杨静怡老师邀请您来 一起找到绽放优雅的秘密").getConfigValue();
            result.put("offerDesc", StringUtils.isNotBlank(offerDto.getOfferDesc())?offerDto.getOfferDesc():desc);
            result.put("pic", offerDto.getOfferPic());
            // 页面展现按钮
            JSONArray btns = new JSONArray();
            String buyPriviStr = offerDto.getBuyPrivi();
            // 判断视频是否可播放，获取页面展现的按钮 1/用户已购买此套餐，2/此套餐对VIP/SVIP用户免费
            boolean canPlay = dealOfferBtn("OFFER", custDto.getCustId(), custDto.getUserLevel(), offerId, buyPriviStr, btns);

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
                /*if(childOffer.getOfferType().equals("CLASS")){
                    // 获取乐视视频ID
                    BaseResult<VideoConfigDto> videoConfigDtoBaseResult = videoConfigService.queryVideo(childOffer.getMainMedia());
                    if(videoConfigDtoBaseResult.getCode()==BaseConstant.FAILED || videoConfigDtoBaseResult.getData()==null){
                        return new BaseResult(BaseConstant.FAILED, "获取视频失败");
                    }
                    if(StringUtils.isBlank(mainMedia)){
                        mainMedia = videoConfigDtoBaseResult.getData().getVideoUnique();
                    }
                    ofrJson.put("mainMedia", videoConfigDtoBaseResult.getData().getVideoUnique());
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
                }*/
                ofrJson.put("offerId", childOffer.getOfferId());
                ofrJson.put("offerName", childOffer.getOfferName());
                ofrJson.put("offerType", childOffer.getOfferType());
                ofrJson.put("pic", childOffer.getOfferPic());
                ofrJson.put("price", childOffer.getSaleMoney());
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
    public BaseResult activeDetail(HttpServletRequest request, String activeId){
        String custId = CookieUtil.getCustId(request);
        CustDto custDto = custService.queryCustByCustId(custId).getData();
        BaseResult<ActiveDto> activeDtoBaseResult = activeService.queryActive(activeId);
        if(activeDtoBaseResult.getCode()== BaseConstant.FAILED){
            return activeDtoBaseResult;
        }
        ActiveDto activeDto = activeDtoBaseResult.getData();
        if(activeDto==null){
            return new BaseResult(BaseConstant.FAILED, "活动不存在");
        }
        if(activeDto.getStatus()==0){
            return new BaseResult(BaseConstant.FAILED, "活动未发布");
        }
        // 判断是套餐还是课程 商品类型 服务类型：VIP,  课程：CLASS, 套餐：OFFER
        JSONObject result = new JSONObject();
        // 基本信息
        result.put("htmlId", activeDto.getHtmlId());
        result.put("activeName", activeDto.getActiveName());
        String desc = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_COM, "APP_SHARE_DESC",
                "杨静怡老师邀请您来 一起找到绽放优雅的秘密").getConfigValue();
        result.put("activeDesc", StringUtils.isNotBlank(activeDto.getActiveDesc())?activeDto.getActiveDesc():desc);
        result.put("activeStartTime", DateUtils.formatHms(activeDto.getActiveStartTime()));
        result.put("activeEndTime", DateUtils.formatHms(activeDto.getActiveEndTime()));
        result.put("pic", activeDto.getPic());
        String province = activeDto.getProvince();
        String provinceName = (String) provinceAndCityService.query("P", province).getData();
        String city = activeDto.getCity();
        String cityName = (String)provinceAndCityService.query("C", city).getData();
        result.put("addr", (provinceName!=null?provinceName + "-":"") + (cityName!=null?cityName + ",":"") + activeDto.getAddr());
        String metaData = activeDto.getMetaData();
        if(StringUtils.isNotBlank(metaData)) {
            JSONObject metaDataJson = JSON.parseObject(metaData);
            Integer limit = metaDataJson.getInteger("LIMIT");
            result.put("limit", limit==null?100:limit);//默认一百人
        }
        result.put("pic", activeDto.getPic());
        // 判断活动是否已经加入
        result.put("isJoin", activeCustListService.hasJoin(activeId, custDto.getCustId()));
        // 页面展现按钮
        JSONArray btns = new JSONArray();
        String buyPriviStr = activeDto.getActivePrivi();
        dealOfferBtn("ACTIVE", custDto.getCustId(), custDto.getUserLevel(), activeId, buyPriviStr, btns);
        result.put("btns", btns);
        return new BaseResult(result);
    }
    @RequestMapping("/api/offerClass/detail")
    @ResponseBody
    public BaseResult classDetail(HttpServletRequest request, String offerId){
        String custId = CookieUtil.getCustId(request);
        CustDto custDto = custService.queryCustByCustId(custId).getData();
        BaseResult<OfferDto> offerDtoBaseResult = offerService.queryOffer(offerId);
        if(offerDtoBaseResult.getCode()== BaseConstant.FAILED){
            return offerDtoBaseResult;
        }
        OfferDto offerDto = offerDtoBaseResult.getData();
        if(offerDto==null){
            return new BaseResult(BaseConstant.FAILED, "课程不存在");
        }
        if(offerDto.getStatus()==0){
            return new BaseResult(BaseConstant.FAILED, "课程已下架");
        }
        // 判断是套餐还是课程 商品类型 服务类型：VIP,  课程：CLASS, 套餐：OFFER
        String type = offerDto.getOfferType();
        if("CLASS".equals(type)){
            JSONObject result = new JSONObject();
            // 基本信息
            result.put("htmlId", offerDto.getHtmlId());
            result.put("offerName", offerDto.getOfferName());
            String desc = ConfigUtil.getConfig(BaseConstant.CONFIG_TYPE_COM, "APP_SHARE_DESC",
                    "杨静怡老师邀请您来 一起找到绽放优雅的秘密").getConfigValue();
            result.put("offerDesc", StringUtils.isNotBlank(offerDto.getOfferDesc())?offerDto.getOfferDesc():desc);
            // 页面展现按钮
            JSONArray btns = new JSONArray();
            String buyPriviStr = offerDto.getBuyPrivi();
            // 判断视频是否可播放，获取页面展现的按钮 1/用户已购买此套餐，2/此套餐对VIP/SVIP用户免费
            boolean canPlay = dealOfferBtn("CLASS", custDto.getCustId(), custDto.getUserLevel(), offerId, buyPriviStr, btns);

            result.put("isPlay", (canPlay?1:0));
            result.put("btns", btns);
            // 获取视频链接
            BaseResult<String> videoUrlBaseResult = videoConfigService.queryUrl(offerDto.getMainMedia(),"m3u8_hd");
            if(videoUrlBaseResult.getCode()==BaseConstant.FAILED){
                return new BaseResult(BaseConstant.FAILED, videoUrlBaseResult.getMsg());
            }
            String videoUrl = videoUrlBaseResult.getData();
            result.put("mainMedia", videoUrl);
            result.put("mediaType", videoUrl.substring(videoUrl.lastIndexOf(".") + 1));
            result.put("pic", offerDto.getOfferPic());
            result.put("videoId", offerDto.getMainMedia());
            String metaDataStr = offerDto.getMetaData();
            if(StringUtils.isNotBlank(metaDataStr)) {
                JSONObject metaData = JSON.parseObject(metaDataStr);
                if (metaData.containsKey("TEACHER")) {
                    result.put("teacher", metaData.getJSONArray("TEACHER"));
                } else {
                    result.put("teacher", null);
                }
            } else {
                result.put("teacher", null);
            }
            return new BaseResult(result);
        } else {
            return new BaseResult(BaseConstant.FAILED, "当前商品非课程");
        }
    }

    private JSONObject createBtn(String code, String name, Integer click, String offerId, String price){
        JSONObject buyBtn = new JSONObject();
        buyBtn.put("click", click);
        buyBtn.put("code", code);
        buyBtn.put("name", name);
        buyBtn.put("offerId", offerId);
        buyBtn.put("price", price);
        return buyBtn;
    }

    private OfferDto queryVipInfo(String type){
        String offerId;
        String key ;
        if(type.equals("VIP")) {
            offerId = "OFFER001";
            key = "OFFER_VIP";
        } else {
            offerId = "OFFER002";
            key = "OFFER_SVIP";
        }
        OfferDto offerDto = null;
        String offerStr = jedisUtil.get(key);
        if(StringUtils.isBlank(offerStr)){
            // 从数据库获取
            offerDto = offerService.queryOffer(offerId).getData();
            jedisUtil.set(key, JSONObject.toJSONString(offerDto), 60*60);// 一小时有效期
        } else {
            offerDto = JSONObject.parseObject(offerStr, OfferDto.class);
        }
        return offerDto;
    }

    private boolean dealOfferBtn(String type, String custId, String lv, String offerId, String buyPriviStr, JSONArray btns){
        boolean canPlay ;
        String str = type.equals("ACTIVE")?"报名":"购买";
        // 判断是否购买过
        if(hasBuy(custId, offerId, type)){
            // 判断是否购买过CLASS/ACTIVE或包含CLASS/ACTIVE的套餐
            canPlay = true;
            btns.add(createBtn(SHARE_FRIEND, "分享好友一起学习", 1, offerId, null));
        } else {
            // 如果没有购买过，判断是否可以购买
            if (StringUtils.isBlank(buyPriviStr)) {
                // 不能单独购买
                canPlay = false;
                btns.add(createBtn(CANNOT_BUY, "不能直接" + str, 0, offerId, null));
            } else {
                // 可以购买
                JSONObject buyPrivi = JSON.parseObject(buyPriviStr);
                if (lv.equals("VIP") || lv.equals("SVIP")) {
                    // 会员
                    Object price = buyPrivi.get(lv);
                    if (price == null) {
                        // 未配置vip的价格，不能购买
                        canPlay = false;
                        if (lv.equals("VIP")) {
                            // 查看SVIP是否可以购买
                            Object svipPrice = buyPrivi.get("SVIP");
                            if (svipPrice != null && new BigDecimal(svipPrice.toString()).compareTo(BigDecimal.ZERO)==0) {
                                // 查询SVIP信息
                                OfferDto offerDto = queryVipInfo("SVIP");
                                String svipBuyPrivi = offerDto.getBuyPrivi();
                                JSONObject svipBuyJson = JSONObject.parseObject(svipBuyPrivi);
                                btns.add(createBtn(BE_SVIP, "升级SVIP", 1,
                                        "OFFER002", svipBuyJson.get(lv).toString()));
                            } else {
                                btns.add(createBtn(CANNOT_BUY, "不能直接" + str, 0, offerId, null));
                            }
                        } else {
                            btns.add(createBtn(CANNOT_BUY, "不能直接" + str, 0, offerId, null));
                        }
                    } else if (new BigDecimal(price.toString()).compareTo(BigDecimal.ZERO) <= 0) {
                        // 免费
                        canPlay = true;
                        if(type.equals("ACTIVE")){
                            // 活动0元也可以报名
                            btns.add(createBtn(BUY_NOW, "立即" + str, 1, offerId,  "0"));
                        }
                        btns.add(createBtn(SHARE_FRIEND, "分享好友一起学习", 1, offerId, null));
                    } else {
                        // 需要付费购买
                        canPlay = false;
                        btns.add(createBtn(BUY_NOW, "立即" + str, 1, offerId, price + ""));
                        if (lv.equals("VIP")) {
                            Object svipPrice = buyPrivi.get("SVIP");
                            if(svipPrice!=null && new BigDecimal(svipPrice.toString()).compareTo(BigDecimal.ZERO)==0) {
                                OfferDto offerDto = queryVipInfo("SVIP");
                                String svipBuyPrivi = offerDto.getBuyPrivi();
                                JSONObject svipBuyJson = JSONObject.parseObject(svipBuyPrivi);
                                btns.add(createBtn(BE_SVIP, "升级SVIP", 1,
                                        "OFFER002", svipBuyJson.get(lv).toString()));
                            }
                        }
                    }
                } else {
                    // 非会员
                    Object price = buyPrivi.get(lv);
                    if (price == null) {
                        canPlay = false;
                        int cannotBuy = 0;
                        if (buyPrivi.containsKey("VIP")) {
                            OfferDto offerDto = queryVipInfo("VIP");
                            String vipBuyPrivi = offerDto.getBuyPrivi();
                            JSONObject vipBuyJson = JSONObject.parseObject(vipBuyPrivi);
                            btns.add(createBtn(BE_VIP, "升级VIP", 1,
                                    "OFFER001-OFFER002", vipBuyJson.get(lv).toString()));
                            cannotBuy++;
                        }
                        if (buyPrivi.containsKey("SVIP")) {
                            OfferDto offerDto = queryVipInfo("SVIP");
                            String vipBuyPrivi = offerDto.getBuyPrivi();
                            JSONObject vipBuyJson = JSONObject.parseObject(vipBuyPrivi);
                            btns.add(createBtn(BE_SVIP, "升级SVIP", 1,
                                    "OFFER002", vipBuyJson.get(lv).toString()));
                            cannotBuy++;
                        }
                        if (cannotBuy == 0) {
                            btns.add(createBtn(CANNOT_BUY, "不能直接" + str, 0, offerId, null));
                        }
                    } else if (new BigDecimal(price.toString()).compareTo(BigDecimal.ZERO) <= 0) {
                        // 免费
                        canPlay = true;
                        if(type.equals("ACTIVE")){
                            // 活动0元也可以报名
                            btns.add(createBtn(BUY_NOW, "立即" + str, 1, offerId,  "0"));
                        }
                        btns.add(createBtn(SHARE_FRIEND, "分享好友一起学习", 1, offerId, null));
                    } else {
                        // 需要购买
                        canPlay = false;
                        btns.add(createBtn(BUY_NOW, "立即" + str, 1, offerId, price + ""));
                        if (buyPrivi.containsKey("VIP") && new BigDecimal(buyPrivi.get("VIP").toString()).compareTo(BigDecimal.ZERO)==0) {
                            OfferDto offerDto = queryVipInfo("VIP");
                            String vipBuyPrivi = offerDto.getBuyPrivi();
                            JSONObject vipBuyJson = JSONObject.parseObject(vipBuyPrivi);
                            btns.add(createBtn(BE_VIP, "升级VIP", 1,
                                    "OFFER001-OFFER002", vipBuyJson.get(lv).toString()));
                        } else if (buyPrivi.containsKey("SVIP") && new BigDecimal(buyPrivi.get("SVIP").toString()).compareTo(BigDecimal.ZERO)==0) {
                            OfferDto offerDto = queryVipInfo("SVIP");
                            String vipBuyPrivi = offerDto.getBuyPrivi();
                            JSONObject vipBuyJson = JSONObject.parseObject(vipBuyPrivi);
                            btns.add(createBtn(BE_SVIP, "升级SVIP", 1,
                                    "OFFER002", vipBuyJson.get(lv).toString()));
                        }
                    }
                }
            }
        }
        return canPlay;
    }

    private boolean hasBuy(String custId, String offerId, String type){
        List<String> offerIds = new ArrayList<>();
        offerIds.add(offerId);
        // 校验是否购买过此套餐
        if(type.equals("CLASS") || type.equals("ACTIVE")){
            // 查询所属套餐
            BaseResult<List<String>> relOfferListBaseResult = offerService.queryOfferByChildOffer(offerId);
            if(relOfferListBaseResult.getCode()==BaseConstant.SUCCESS && CollectionUtils.isNotEmpty(relOfferListBaseResult.getData())){
                offerIds.addAll(relOfferListBaseResult.getData());
            }
        }
        for(String _offerId : offerIds) {
            PurchasedInfoDto purchasedVideoDto = new PurchasedInfoDto();
            purchasedVideoDto.setCustId(custId);
            purchasedVideoDto.setOfferId(_offerId);
            BaseResult<Integer> hasBuy = purchasedInfoService.count(purchasedVideoDto);
            if (hasBuy.getCode() == BaseConstant.FAILED) {
                throw new RuntimeException(hasBuy.getMsg());
            }
            if(hasBuy.getData() > 0) {
                return true;
            }
        }
        return false;
    }
}
