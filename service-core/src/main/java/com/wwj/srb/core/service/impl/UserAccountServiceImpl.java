package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.common.result.ResponseEnum;
import com.wwj.common.result.exception.Assert;
import com.wwj.srb.core.enums.TransTypeEnum;
import com.wwj.srb.core.hfb.FormHelper;
import com.wwj.srb.core.hfb.HfbConst;
import com.wwj.srb.core.hfb.RequestHelper;
import com.wwj.srb.core.mapper.UserAccountMapper;
import com.wwj.srb.core.mapper.UserInfoMapper;
import com.wwj.srb.core.pojo.bo.TransFlowBO;
import com.wwj.srb.core.pojo.entity.UserAccount;
import com.wwj.srb.core.pojo.entity.UserInfo;
import com.wwj.srb.core.service.TransFlowService;
import com.wwj.srb.core.service.UserAccountService;
import com.wwj.srb.core.service.UserBindService;
import com.wwj.srb.core.util.LendNoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Slf4j
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Resource
    private UserInfoMapper userInfoMapper;
    @Autowired
    private TransFlowService transFlowService;
    @Resource
    private UserBindService userBindService;
    @Resource
    private UserAccountService userAccountService;

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String notify(Map<String, Object> paramMap) {
        String agentBillNo = (String) paramMap.get("agentBillNo");
        // 幂等性判断（判断交易流水是否存在）
        boolean isSave = transFlowService.isSaveTransFlow(agentBillNo);
        if (isSave) {
            log.warn("幂等性返回");
            return "success";
        }
        // 账户处理
        String bindCode = (String) paramMap.get("bindCode");
        String chargeAmt = (String) paramMap.get("chargeAmt");
        baseMapper.updateAccount(bindCode, new BigDecimal(chargeAmt), new BigDecimal(0));

        // 记录账户流水
        TransFlowBO transFlowBO = new TransFlowBO(
                agentBillNo,
                bindCode,
                new BigDecimal(chargeAmt),
                TransTypeEnum.RECHARGE,
                "充值");
        transFlowService.saveTransFlow(transFlowBO);
        return "success";
    }

    @Override
    public BigDecimal getAccount(Long userId) {
        UserAccount userAccount = baseMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getUserId, userId));
        return userAccount.getAmount();
    }

    @Override
    public String commitWithdraw(BigDecimal fetchAmt, Long userId) {
        //账户可用余额充足：当前用户的余额 >= 当前用户的提现金额
        BigDecimal amount = userAccountService.getAccount(userId);//获取当前用户的账户余额
        Assert.isTrue(amount.doubleValue() >= fetchAmt.doubleValue(),
                ResponseEnum.NOT_SUFFICIENT_FUNDS_ERROR);

        String bindCode = userBindService.getBindCodeByUserId(userId);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentBillNo", LendNoUtils.getWithdrawNo());
        paramMap.put("bindCode", bindCode);
        paramMap.put("fetchAmt", fetchAmt);
        paramMap.put("feeAmt", new BigDecimal(0));
        paramMap.put("notifyUrl", HfbConst.WITHDRAW_NOTIFY_URL);
        paramMap.put("returnUrl", HfbConst.WITHDRAW_RETURN_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        String sign = RequestHelper.getSign(paramMap);
        paramMap.put("sign", sign);

        //构建自动提交表单
        return FormHelper.buildForm(HfbConst.WITHDRAW_URL, paramMap);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void notifyWithdraw(Map<String, Object> paramMap) {
        log.info("提现成功");
        String agentBillNo = (String) paramMap.get("agentBillNo");
        boolean result = transFlowService.isSaveTransFlow(agentBillNo);
        if (result) {
            log.warn("幂等性返回");
            return;
        }

        String bindCode = (String) paramMap.get("bindCode");
        String fetchAmt = (String) paramMap.get("fetchAmt");

        //根据用户账户修改账户金额
        baseMapper.updateAccount(bindCode, new BigDecimal("-" + fetchAmt), new BigDecimal(0));

        //增加交易流水
        TransFlowBO transFlowBO = new TransFlowBO(
                agentBillNo,
                bindCode,
                new BigDecimal(fetchAmt),
                TransTypeEnum.WITHDRAW,
                "提现");
        transFlowService.saveTransFlow(transFlowBO);
    }
}
