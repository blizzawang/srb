package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.common.result.ResponseEnum;
import com.wwj.common.result.exception.Assert;
import com.wwj.srb.core.enums.LendStatusEnum;
import com.wwj.srb.core.enums.TransTypeEnum;
import com.wwj.srb.core.hfb.FormHelper;
import com.wwj.srb.core.hfb.HfbConst;
import com.wwj.srb.core.hfb.RequestHelper;
import com.wwj.srb.core.mapper.LendItemMapper;
import com.wwj.srb.core.mapper.LendMapper;
import com.wwj.srb.core.mapper.UserAccountMapper;
import com.wwj.srb.core.pojo.bo.TransFlowBO;
import com.wwj.srb.core.pojo.entity.Lend;
import com.wwj.srb.core.pojo.entity.LendItem;
import com.wwj.srb.core.pojo.vo.InvestVO;
import com.wwj.srb.core.service.*;
import com.wwj.srb.core.util.LendNoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 标的出借记录表 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
public class LendItemServiceImpl extends ServiceImpl<LendItemMapper, LendItem> implements LendItemService {

    @Resource
    private LendMapper lendMapper;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private UserBindService userBindService;
    @Autowired
    private LendService lendService;
    @Autowired
    private TransFlowService transFlowService;
    @Resource
    private UserAccountMapper userAccountMapper;

    @Override
    public String commitInvest(InvestVO investVO) {

        //输入校验==========================================
        Long lendId = investVO.getLendId();
        //获取标的信息
        Lend lend = lendMapper.selectById(lendId);

        //标的状态必须为募资中
        Assert.isTrue(lend.getStatus().intValue() == LendStatusEnum.INVEST_RUN.getStatus().intValue(), ResponseEnum.LEND_INVEST_ERROR);

        //标的不能超卖：(已投金额 + 本次投资金额 )>=标的金额（超卖）
        BigDecimal sum = lend.getInvestAmount().add(new BigDecimal(investVO.getInvestAmount()));
        Assert.isTrue(sum.doubleValue() <= lend.getAmount().doubleValue(), ResponseEnum.LEND_FULL_SCALE_ERROR);

        //账户可用余额充足：当前用户的余额 >= 当前用户的投资金额（可以投资）
        Long investUserId = investVO.getInvestUserId();
        BigDecimal amount = userAccountService.getAccount(investUserId);//获取当前用户的账户余额
        Assert.isTrue(amount.doubleValue() >= Double.parseDouble(investVO.getInvestAmount()), ResponseEnum.NOT_SUFFICIENT_FUNDS_ERROR);

        //在商户平台中生成投资信息==========================================
        //标的下的投资信息
        LendItem lendItem = new LendItem();
        lendItem.setInvestUserId(investUserId);//投资人id
        lendItem.setInvestName(investVO.getInvestName());//投资人名字
        String lendItemNo = LendNoUtils.getLendItemNo();
        lendItem.setLendItemNo(lendItemNo); //投资条目编号（一个Lend对应一个或多个LendItem）
        lendItem.setLendId(investVO.getLendId());//对应的标的id
        lendItem.setInvestAmount(new BigDecimal(investVO.getInvestAmount())); //此笔投资金额
        lendItem.setLendYearRate(lend.getLendYearRate());//年化
        lendItem.setInvestTime(LocalDateTime.now()); //投资时间
        lendItem.setLendStartDate(lend.getLendStartDate()); //开始时间
        lendItem.setLendEndDate(lend.getLendEndDate()); //结束时间

        //预期收益
        BigDecimal expectAmount = lendService.getInterestCount(
                lendItem.getInvestAmount(),
                lendItem.getLendYearRate(),
                lend.getPeriod(),
                lend.getReturnMethod());
        lendItem.setExpectAmount(expectAmount);

        //实际收益
        lendItem.setRealAmount(new BigDecimal(0));

        lendItem.setStatus(0);//默认状态：刚刚创建
        baseMapper.insert(lendItem);


        //组装投资相关的参数，提交到汇付宝资金托管平台==========================================
        //在托管平台同步用户的投资信息，修改用户的账户资金信息==========================================
        //获取投资人的绑定协议号
        String bindCode = userBindService.getBindCodeByUserId(investUserId);
        //获取借款人的绑定协议号
        String benefitBindCode = userBindService.getBindCodeByUserId(lend.getUserId());

        //封装提交至汇付宝的参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("voteBindCode", bindCode);
        paramMap.put("benefitBindCode", benefitBindCode);
        paramMap.put("agentProjectCode", lend.getLendNo());//项目标号
        paramMap.put("agentProjectName", lend.getTitle());

        //在资金托管平台上的投资订单的唯一编号，要和lendItemNo保持一致。
        paramMap.put("agentBillNo", lendItemNo);//订单编号
        paramMap.put("voteAmt", investVO.getInvestAmount());
        paramMap.put("votePrizeAmt", "0");
        paramMap.put("voteFeeAmt", "0");
        paramMap.put("projectAmt", lend.getAmount()); //标的总金额
        paramMap.put("note", "");
        paramMap.put("notifyUrl", HfbConst.INVEST_NOTIFY_URL); //检查常量是否正确
        paramMap.put("returnUrl", HfbConst.INVEST_RETURN_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        String sign = RequestHelper.getSign(paramMap);
        paramMap.put("sign", sign);

        //构建充值自动提交表单
        return FormHelper.buildForm(HfbConst.INVEST_URL, paramMap);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void notify(Map<String, Object> paramMap) {
        // 幂等性返回
        String agentBillNo = (String) paramMap.get("agentBillNo");
        boolean result = transFlowService.isSaveTransFlow(agentBillNo);
        if (result) {
            log.warn("幂等性返回");
            return;
        }

        // 修改账户金额，从余额中减去投资金额，在冻结金额中加上投资金额
        String voteBindCode = (String) paramMap.get("voteBindCode");
        String voteAmt = (String) paramMap.get("voteAmt");
        userAccountMapper.updateAccount(
                voteBindCode,
                new BigDecimal("-" + voteAmt),
                new BigDecimal(voteAmt));

        // 修改投资记录的状态
        LendItem lendItem = this.getByLendItemNo(agentBillNo);
        lendItem.setStatus(1);
        baseMapper.updateById(lendItem);

        // 修改标的记录（投资人数、已投金额）
        Long lendId = lendItem.getLendId();
        Lend lend = lendMapper.selectById(lendId);
        lend.setInvestNum(lend.getInvestNum() + 1);
        lend.setInvestAmount(lend.getInvestAmount().add(lendItem.getInvestAmount()));
        lendMapper.updateById(lend);

        // 新增交易流水
        TransFlowBO transFlowBO = new TransFlowBO(
                agentBillNo,
                voteBindCode,
                new BigDecimal(voteAmt),
                TransTypeEnum.INVEST_LOCK,
                "项目编号:" + lend.getLendNo() + ",项目名称:" + lend.getTitle());
        transFlowService.saveTransFlow(transFlowBO);
    }

    @Override
    public List<LendItem> selectByLendId(Long lendId, Integer status) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<LendItem>()
                        .eq(LendItem::getLendId, lendId)
                        .eq(LendItem::getStatus, status));
    }

    @Override
    public List<LendItem> selectByLendId(Long lendId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<LendItem>()
                        .eq(LendItem::getLendId, lendId));
    }

    /**
     * 根据流水号获取投资记录
     *
     * @param lendItemNo
     * @return
     */
    private LendItem getByLendItemNo(String lendItemNo) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<LendItem>()
                        .eq(LendItem::getLendItemNo, lendItemNo));
    }
}
