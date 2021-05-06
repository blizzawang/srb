package com.wwj.srb.sms.service.impl;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;
import com.wwj.common.result.ResponseEnum;
import com.wwj.common.result.exception.Assert;
import com.wwj.common.result.exception.BusinessException;
import com.wwj.srb.sms.service.SmsService;
import com.wwj.srb.sms.util.SmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    /**
     * 发送验证码短信
     *
     * @param mobile
     * @param templateCode
     * @param param
     */
    @Override
    public void send(String mobile, String templateCode, Map<String, Object> param) {
        // 创建远程客户端
        DefaultProfile defaultProfile = DefaultProfile.getProfile(
                SmsProperties.REGION_ID,
                SmsProperties.kEY_ID,
                SmsProperties.KEY_SECRET);
        IAcsClient client = new DefaultAcsClient(defaultProfile);
        // 创建远程连接的请求参数
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", SmsProperties.REGION_ID);
        request.putQueryParameter("PhoneNumbers", mobile);
        request.putQueryParameter("SignName", SmsProperties.SIGN_NAME);
        request.putQueryParameter("TemplateCode", SmsProperties.TEMPLATE_CODE);
        Gson gson = new Gson();
        String jsonParam = gson.toJson(param);
        request.putQueryParameter("TemplateParam", jsonParam);
        try {
            // 使用客户端对象携带请求参数向阿里云服务器发起远程调用
            CommonResponse response = client.getCommonResponse(request);
            System.out.println("response.getData():" + response.getData());

            boolean success = response.getHttpResponse().isSuccess();
            // 通信失败，进行错误处理
            Assert.isTrue(success, ResponseEnum.ALIYUN_RESPONSE_ERRPR);

            // 获取响应结果
            String data = response.getData();
            HashMap<String, String> resultMap = gson.fromJson(data, HashMap.class);
            String code = resultMap.get("Code");
            String message = resultMap.get("Message");
            log.info("code:" + code + ",message" + message);

            // 业务处理失败
            // 短信发送频繁，进行错误处理，默认一分钟内仅能发送一条短信
            Assert.notEquals("isv.BUSINESS_LIMIT_CONTROL", code, ResponseEnum.ALIYUN_SMS_LIMIT_CONTROL_ERROR);
            // 短信发送失败，进行错误处理
            Assert.equals("OK", code, ResponseEnum.ALIYUN_SMS_ERROR);
        } catch (ClientException e) {
            log.error("阿里云短信服务调用失败:" + e.getErrCode() + "," + e.getErrMsg());
            throw new BusinessException(ResponseEnum.ALIYUN_SMS_ERROR, e);
        }
    }
}
