package com.wwj.srb.core.service;

import com.wwj.srb.core.pojo.entity.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Map;

/**
 * <p>
 * 用户账户 服务类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
public interface UserAccountService extends IService<UserAccount> {

    String commitCharge(BigDecimal chargeAmt, Long userId);

    String notify(Map<String, Object> paramMap);
}
