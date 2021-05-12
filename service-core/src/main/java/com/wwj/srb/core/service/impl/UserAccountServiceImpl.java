package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.srb.core.hfb.FormHelper;
import com.wwj.srb.core.hfb.HfbConst;
import com.wwj.srb.core.hfb.RequestHelper;
import com.wwj.srb.core.mapper.UserAccountMapper;
import com.wwj.srb.core.mapper.UserInfoMapper;
import com.wwj.srb.core.pojo.entity.UserAccount;
import com.wwj.srb.core.pojo.entity.UserInfo;
import com.wwj.srb.core.service.UserAccountService;
import com.wwj.srb.core.util.LendNoUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户账户 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Override
    public String commitCharge(BigDecimal chargeAmt, Long userId) {
        // 获取充值人绑定协议号
        UserInfo userInfo = userInfoMapper.selectById(userId);
        String bindCode = userInfo.getBindCode();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentBillNo", LendNoUtils.getChargeNo());
        paramMap.put("bindCode", bindCode);
        paramMap.put("chargeAmt", chargeAmt);
        paramMap.put("fee_amt", new BigDecimal("0"));
        paramMap.put("notifyUrl", HfbConst.RECHARGE_NOTIFY_URL);
        paramMap.put("returnUrl", HfbConst.RECHARGE_RETURN_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        paramMap.put("sign", RequestHelper.getSign(paramMap));

        return FormHelper.buildForm(HfbConst.RECHARGE_URL, paramMap);
    }

    @Override
    public String notify(Map<String, Object> paramMap) {
        // 幂等性判断（判断交易流水是否存在）

        // 账户处理
        String bindCode = (String) paramMap.get("bindCode");
        String chargeAmt = (String) paramMap.get("chargeAmt");
        baseMapper.updateAccount(bindCode, new BigDecimal(chargeAmt), new BigDecimal(0));

        // 记录账户流水

        return "success";
    }
}
