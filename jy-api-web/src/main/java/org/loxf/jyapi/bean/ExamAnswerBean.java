package org.loxf.jyapi.bean;

import java.util.List;
import java.util.Map;

public class ExamAnswerBean {
    List<Map<String, String>> answers;
    String offerId;

    public List<Map<String, String>> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Map<String, String>> answers) {
        this.answers = answers;
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }
}
