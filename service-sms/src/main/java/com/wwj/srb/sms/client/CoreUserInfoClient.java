package com.wwj.srb.sms.client;

import com.wwj.srb.sms.client.follback.CoreUserInfoClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 远程调用service-core服务接口
 */
@FeignClient(value = "service-core", fallback = CoreUserInfoClientFallback.class)
public interface CoreUserInfoClient {

    /**
     * 校验手机号是否已经被注册
     *
     * @param mobile
     * @return
     */
    // 请求路径必须填写完整
    @GetMapping("/api/core/userInfo/checkMobile/{mobile}")
    boolean checkMobile(@PathVariable String mobile);
}
