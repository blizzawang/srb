package com.wwj.srb.sms.controller.api;

import com.wwj.common.result.R;
import com.wwj.common.result.ResponseEnum;
import com.wwj.common.result.exception.Assert;
import com.wwj.common.util.RandomUtils;
import com.wwj.common.util.RegexValidateUtils;
import com.wwj.srb.sms.client.CoreUserInfoClient;
import com.wwj.srb.sms.service.SmsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/sms")
@Api(tags = "短信管理")
//@CrossOrigin //跨域
@Slf4j
public class ApiSmsController {

    @Autowired
    private SmsService smsService;
    @Resource
    private RedisTemplate redisTemplate;
    @Autowired
    private CoreUserInfoClient coreUserInfoClient;

    @ApiOperation("获取验证码")
    @GetMapping("/send/{mobile}")
    public R send(
            @ApiParam(value = "手机号", required = true)
            @PathVariable String mobile) {
        // 校验手机号是否为空
        Assert.notEmpty(mobile, ResponseEnum.MOBILE_NULL_ERROR);
        // 校验手机号是否合法
        Assert.isTrue(RegexValidateUtils.checkCellphone(mobile), ResponseEnum.MOBILE_ERROR);

        // 按断手机号是否已经被注册（这是一次远程调用）
        boolean result = coreUserInfoClient.checkMobile(mobile);
        Assert.isTrue(!result, ResponseEnum.MOBILE_EXIST_ERROR);

        // 随机出一个四位数的验证码
        Map<String, Object> map = new HashMap<>();
        String code = RandomUtils.getFourBitRandom();
        map.put("code", code);
//        smsService.send(mobile, SmsProperties.TEMPLATE_CODE, map); // 调用阿里云短信服务发送短信

        // 将验证码存入Redis，过期时间为5分钟
        redisTemplate.opsForValue().set("srb:sms:code:" + mobile, code, 5, TimeUnit.MINUTES);
        return R.ok().message("短信发送成功");
    }
}
