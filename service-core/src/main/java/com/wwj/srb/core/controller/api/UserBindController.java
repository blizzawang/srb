package com.wwj.srb.core.controller.api;


import com.alibaba.fastjson.JSON;
import com.wwj.common.result.R;
import com.wwj.srb.base.util.JwtUtils;
import com.wwj.srb.core.hfb.RequestHelper;
import com.wwj.srb.core.pojo.vo.UserBindVO;
import com.wwj.srb.core.service.UserBindService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * <p>
 * 用户绑定表 前端控制器
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Api(tags = "会员账号绑定")
@RestController
@RequestMapping("/api/core/userBind")
@Slf4j
public class UserBindController {

    @Autowired
    private UserBindService userBindService;

    @ApiOperation("账户绑定提交数据")
    @PostMapping("/auth/bind")
    public R bind(@RequestBody UserBindVO userBindVO, HttpServletRequest request) {
        // 从header中获取token，并对token进行校验（确保用户处于登录状态）
        String token = request.getHeader("token");
        // 从token中获取userId
        Long userId = JwtUtils.getUserId(token);
        // 根据userId进行账户绑定
        // 生成一个动态表单的字符串
        String formStr = userBindService.commitBindUser(userBindVO, userId);
        return R.ok().data("formStr", formStr);
    }

    @ApiOperation("账户绑定异步回调")
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        // 获取汇付宝提交过来的参数
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());
        boolean result = RequestHelper.isSignEquals(paramMap);
        // 校验签名
        if (!result) {
            log.error("用户账户绑定异步回调签名验证错误:" + JSON.toJSONString(paramMap));
            return "fail";
        }
        log.info("验证签名成功，开始账户绑定......");
        userBindService.notify(paramMap);
        // 汇付宝有重试机制，默认为5次，当接收到"success"字符串时会停止重试并认为响应成功
        return "success";
    }
}

