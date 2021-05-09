package com.wwj.srb.sms.client.follback;

import com.wwj.srb.sms.client.CoreUserInfoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 当远程服务调用失败后，进入备选方案
 */
@Service
@Slf4j
public class CoreUserInfoClientFallback implements CoreUserInfoClient {

    /**
     * 当上游服务（校验手机号是否已经被注册）被熔断时，执行此备选方案
     *
     * @param mobile
     * @return
     */
    @Override
    public boolean checkMobile(String mobile) {
        // 备选方案为：手机号不重复
        log.error("远程调用失败，服务熔断......");
        return false;
    }
}
