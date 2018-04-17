package org.loxf.jyapi.web;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.loxf.jyadmin.base.bean.BaseResult;
import org.loxf.jyadmin.base.bean.PageResult;
import org.loxf.jyadmin.base.bean.Pager;
import org.loxf.jyadmin.base.constant.BaseConstant;
import org.loxf.jyadmin.client.dto.*;
import org.loxf.jyadmin.client.service.ClassQuestionService;
import org.loxf.jyadmin.client.service.CustCertifyService;
import org.loxf.jyadmin.client.service.CustScoreService;
import org.loxf.jyadmin.client.service.OfferService;
import org.loxf.jyapi.bean.ExamAnswerBean;
import org.loxf.jyapi.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ExamController {
    @Autowired
    private ClassQuestionService classQuestionService;
    @Autowired
    private CustScoreService custScoreService;
    @Autowired
    private CustCertifyService custCertifyService;
    @Autowired
    private OfferService offerService;

    @RequestMapping("/api/exam/getQuestions")
    @ResponseBody
    public BaseResult getQuestions(String offerId){
        JSONObject metaJson = getOfferMeta(offerId);
        if(metaJson!=null && metaJson.containsKey("EXAMENABLE") && metaJson.getBoolean("EXAMENABLE")) {
            BaseResult<List<ClassQuestionDto>> baseResult = classQuestionService.queryQuestions(offerId);
            if (baseResult.getCode() == BaseConstant.SUCCESS && CollectionUtils.isNotEmpty(baseResult.getData())) {
                List<JSONObject> result = new ArrayList<>();
                for (ClassQuestionDto classQuestionDto : baseResult.getData()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("options", JSONObject.parseArray(classQuestionDto.getOptions()));
                    jsonObject.put("pics", (StringUtils.isBlank(classQuestionDto.getPics()) ? "" : classQuestionDto.getPics().split(",")));
                    jsonObject.put("questionId", classQuestionDto.getQuestionId());
                    jsonObject.put("score", classQuestionDto.getScore());
                    jsonObject.put("title", classQuestionDto.getTitle());
                    jsonObject.put("type", (classQuestionDto.getType() == 1 ? "SINGLE" : "MULT"));// 1:单选 2：多选
                    result.add(jsonObject);
                }
                return new BaseResult(result);
            }
            return baseResult;
        } else {
            return new BaseResult(BaseConstant.FAILED, "该课程暂未开放考试");
        }
    }

    @RequestMapping("/api/exam/getScore")
    @ResponseBody
    public BaseResult getScore(HttpServletRequest request , String offerId, String answers){
        String custId = CookieUtil.getCustId(request);
        JSONObject metaJson = getOfferMeta(offerId);
        if(metaJson==null){
            return new BaseResult(BaseConstant.FAILED, "该课程暂未开放考试");
        }
        int passScore = metaJson.getIntValue("EXAMPASS");
        String examName = "";
        BaseResult<List<ClassQuestionDto>> baseResult = classQuestionService.queryQuestions(offerId);
        if(baseResult.getCode()== BaseConstant.SUCCESS && CollectionUtils.isNotEmpty(baseResult.getData())){
            int total = 0, score = 0;
            examName = baseResult.getData().get(0).getExamName();
            Map<String, String> classAnswerMap = new HashMap();
            JSONArray userAnswers = JSONObject.parseArray(answers);
            for(int i=0; i< userAnswers.size(); i++){
                JSONObject jsonObject = userAnswers.getJSONObject(i);
                classAnswerMap.put(jsonObject.getString("questionId"), jsonObject.getString("answer"));
            }
            for(ClassQuestionDto classQuestionDto : baseResult.getData()){
                int currScore = classQuestionDto.getScore();
                String standardAnswer = classQuestionDto.getAnswer();
                String questionId = classQuestionDto.getQuestionId();
                String userAnswer = classAnswerMap.get(questionId);
                if(userAnswer.equals(standardAnswer)){
                    score += currScore;
                }
                total += currScore;
            }
            boolean pass = passScore<=score;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("desc", pass?"恭喜，通过考试":"未通过，" + passScore + "分通过");
            jsonObject.put("score", score);
            // 获取最高和最低两个分数
            BaseResult<String[]> minMaxScore = custScoreService.getMinMaxScore(offerId);
            jsonObject.put("lowest", minMaxScore.getData()[0]);
            jsonObject.put("highest", minMaxScore.getData()[1]);
            // 新增考试成绩
            CustScoreDto custScoreDto = new CustScoreDto();
            custScoreDto.setCustId(custId);
            custScoreDto.setExamName(examName);
            custScoreDto.setIsPass(pass?1:0);
            custScoreDto.setOfferId(offerId);
            custScoreDto.setScore(score);
            custScoreDto.setVersion(System.currentTimeMillis()+"");
            BaseResult result = custScoreService.addScore(custScoreDto);
            result.setData(jsonObject);
            return result;
        }
        return baseResult;
    }

    private JSONObject getOfferMeta(String offerId){
        BaseResult<OfferDto> offerDtoBaseResult = offerService.queryOffer(offerId);
        if(offerDtoBaseResult.getCode()==BaseConstant.FAILED){
            return null;
        }
        OfferDto offerDto = offerDtoBaseResult.getData();
        String metaData = offerDto.getMetaData();
        JSONObject metaJson = JSONObject.parseObject(metaData);
        return metaJson;
    }

    @RequestMapping("/api/cust/myCertify")
    @ResponseBody
    public PageResult<CustCertifyDto> myCertify(HttpServletRequest request, Integer page, Integer size){
        String custId = CookieUtil.getCustId(request);
        CustCertifyDto custCertifyDto = new CustCertifyDto();
        custCertifyDto.setPager(new Pager(page, size));
        custCertifyDto.setCustId(custId);
        return custCertifyService.pager(custCertifyDto);
    }

    @RequestMapping("/api/cust/myScore")
    @ResponseBody
    public PageResult<CustScoreDto> myScore(HttpServletRequest request, Integer page, Integer size){
        String custId = CookieUtil.getCustId(request);
        CustScoreDto custScoreDto = new CustScoreDto();
        custScoreDto.setPager(new Pager(page, size));
        custScoreDto.setCustId(custId);
        return custScoreService.pager(custScoreDto);
    }
}
