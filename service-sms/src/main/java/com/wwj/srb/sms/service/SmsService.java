package com.wwj.srb.sms.service;

import java.util.Map;

public interface SmsService {

    /**
     * 发送验证码短信
     * @param mobile
     * @param templateCode
     * @param param
     */
    void send(String mobile, String templateCode, Map<String,Object> param);
}
