package com.wwj.srb.sms.util;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.sms")
public class SmsProperties implements InitializingBean {

    //    region-id: cn-hangzhou
    //    key-id: LTAI4G5Svnb2TWBMuKnNT6jY
    //    key-secret: N7v6R4V3EJ1SGDZlsqtqo8QyVVMmtQ
    //    template-code: SMS_96695065
    //    sign-name: 谷粒
    private String regionId;
    private String keyId;
    private String keySecret;
    private String templateCode;
    private String signName;

    public static String REGION_ID;
    public static String kEY_ID;
    public static String KEY_SECRET;
    public static String TEMPLATE_CODE;
    public static String SIGN_NAME;


    /**
     * 当属性值被SpringBoot初始化完成之后调用此方法
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 将其保存在常量中
        REGION_ID = regionId;
        kEY_ID = keyId;
        KEY_SECRET = keySecret;
        TEMPLATE_CODE = templateCode;
        SIGN_NAME = signName;
    }
}
