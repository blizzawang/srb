package com.wwj.srb.core.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.common.result.exception.BusinessException;
import com.wwj.srb.core.enums.LendStatusEnum;
import com.wwj.srb.core.enums.ReturnMethodEnum;
import com.wwj.srb.core.enums.TransTypeEnum;
import com.wwj.srb.core.hfb.HfbConst;
import com.wwj.srb.core.hfb.RequestHelper;
import com.wwj.srb.core.mapper.BorrowerMapper;
import com.wwj.srb.core.mapper.LendMapper;
import com.wwj.srb.core.mapper.UserAccountMapper;
import com.wwj.srb.core.mapper.UserInfoMapper;
import com.wwj.srb.core.pojo.bo.TransFlowBO;
import com.wwj.srb.core.pojo.entity.*;
import com.wwj.srb.core.pojo.vo.BorrowInfoApprovalVO;
import com.wwj.srb.core.pojo.vo.BorrowerDetailVO;
import com.wwj.srb.core.service.*;
import com.wwj.srb.core.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 标的准备表 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
@Slf4j
public class LendServiceImpl extends ServiceImpl<LendMapper, Lend> implements LendService {

    @Autowired
    private DictService dictService;
    @Resource
    private BorrowerMapper borrowerMapper;
    @Autowired
    private BorrowerService borrowerService;
    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private UserAccountMapper userAccountMapper;
    @Resource
    private LendItemService lendItemService;
    @Resource
    private TransFlowService transFlowService;
    @Resource
    private LendReturnService lendReturnService;
    @Resource
    private LendItemReturnService lendItemReturnService;

    @Override
    public void createLend(BorrowInfoApprovalVO borrowInfoApprovalVO, BorrowInfo borrowInfo) {
        Lend lend = new Lend();
        lend.setUserId(borrowInfo.getUserId());
        lend.setBorrowInfoId(borrowInfo.getId());
        lend.setLendNo(LendNoUtils.getLendNo());
        lend.setTitle(borrowInfoApprovalVO.getTitle());
        lend.setAmount(borrowInfo.getAmount());
        lend.setPeriod(borrowInfo.getPeriod());
        lend.setLendYearRate(borrowInfoApprovalVO.getLendYearRate().divide(new BigDecimal(100)));
        lend.setServiceRate(borrowInfoApprovalVO.getServiceRate().divide(new BigDecimal(100)));
        lend.setReturnMethod(borrowInfo.getReturnMethod());
        lend.setLowestAmount(new BigDecimal(100));  // 最低投资金额
        lend.setInvestAmount(new BigDecimal(0));    // 已投金额
        lend.setInvestNum(0);    // 已投人数
        lend.setPublishDate(LocalDateTime.now());   // 标的发布时间

        // 计算起息日期
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate lendStartDate = LocalDate.parse(borrowInfoApprovalVO.getLendStartDate(), dateTimeFormatter);
        lend.setLendStartDate(lendStartDate);
        // 计算结算日期
        LocalDate lendEndDate = lendStartDate.plusMonths(borrowInfo.getPeriod());
        lend.setLendEndDate(lendEndDate);

        // 标的描述
        lend.setLendInfo(borrowInfoApprovalVO.getLendInfo());
        // 计算平台预期收益（根据年化率计算平台月化率，再乘以期数，乘以标的金额）
        BigDecimal monthRate = lend.getServiceRate().divide(new BigDecimal(12), 8, BigDecimal.ROUND_DOWN);
        BigDecimal expectAmount = lend.getAmount().multiply(monthRate.multiply(new BigDecimal(lend.getPeriod())));
        lend.setExpectAmount(expectAmount);
        // 实际收益
        lend.setRealAmount(new BigDecimal(0));
        // 设置标的状态
        lend.setStatus(LendStatusEnum.INVEST_RUN.getStatus());
        // 设置审核时间
        lend.setCheckTime(LocalDateTime.now());
        // 设置审核人
        lend.setCheckAdminId(1L);
        // 存入数据表
        baseMapper.insert(lend);
    }

    @Override
    public List<Lend> selectList() {
        List<Lend> lendList = baseMapper.selectList(null);
        lendList.forEach(lend -> {
            String returnMethod = dictService.getNameByParentDictCodeAndValue("returnMethod", lend.getReturnMethod());
            String status = LendStatusEnum.getMsgByStatus(lend.getStatus());
            lend.getParam().put("returnMethod", returnMethod);
            lend.getParam().put("status", status);
        });
        return lendList;
    }

    @Override
    public Map<String, Object> getLendDetail(Long id) {
        // 查询lend
        Lend lend = baseMapper.selectById(id);
        String returnMethod = dictService.getNameByParentDictCodeAndValue("returnMethod", lend.getReturnMethod());
        String status = LendStatusEnum.getMsgByStatus(lend.getStatus());
        lend.getParam().put("returnMethod", returnMethod);
        lend.getParam().put("status", status);

        // 查询借款人对象
        Borrower borrower = borrowerMapper.selectOne(
                new LambdaQueryWrapper<Borrower>()
                        .eq(Borrower::getUserId, lend.getUserId()));
        BorrowerDetailVO borrowerDetailVO = borrowerService.getBorrowerDetailVOById(borrower.getId());
        // 封装集合结果
        Map<String, Object> result = new HashMap<>();
        result.put("lend", lend);
        result.put("borrower", borrowerDetailVO);
        return result;
    }

    @Override
    public BigDecimal getInterestCount(BigDecimal invest, BigDecimal yearRate, Integer totalmonth, Integer returnMethod) {
        if (invest == null) {
            return new BigDecimal(0);
        }
        BigDecimal interestCount = null;
        // 判断还款方式
        if (returnMethod.intValue() == ReturnMethodEnum.ONE.getMethod()) {
            interestCount = Amount1Helper.getInterestCount(invest, yearRate, totalmonth);
        } else if (returnMethod.intValue() == ReturnMethodEnum.TWO.getMethod()) {
            interestCount = Amount2Helper.getInterestCount(invest, yearRate, totalmonth);
        } else if (returnMethod.intValue() == ReturnMethodEnum.THREE.getMethod()) {
            interestCount = Amount3Helper.getInterestCount(invest, yearRate, totalmonth);
        } else if (returnMethod.intValue() == ReturnMethodEnum.FOUR.getMethod()) {
            interestCount = Amount4Helper.getInterestCount(invest, yearRate, totalmonth);
        }
        return interestCount;
    }

    @Override
    public void makeLoan(Long id) {
        // 获取标的信息
        Lend lend = baseMapper.selectById(id);
        // 调用汇付宝放款接口
        Map<String, Object> map = new HashMap<>();
        map.put("agentId", HfbConst.AGENT_ID);
        map.put("agentProjectCode", lend.getLendNo());
        map.put("agentBillNo", LendNoUtils.getLoanNo());
        // 计算月化率
        BigDecimal monthRate = lend.getServiceRate().divide(new BigDecimal(12), 8, BigDecimal.ROUND_DOWN);
        // 平台服务费（已投金额 * 月化率 * 投资时长）
        BigDecimal realAmount = lend.getInvestAmount().multiply(monthRate).multiply(new BigDecimal(lend.getPeriod()));
        map.put("mchFee", realAmount);
        map.put("timestamp", RequestHelper.getTimestamp());
        map.put("sign", RequestHelper.getSign(map));

        // 提交远程请求
        JSONObject result = RequestHelper.sendRequest(map, HfbConst.MAKE_LOAD_URL);
        log.info("放款结果:" + result.toJSONString());
        // 若是放款失败，则抛出异常
        if (!"0000".equals(result.getString("resultCode"))) {
            throw new BusinessException(result.getString("resultMsg"));
        }

        // 标的状态和标的平台收益
        lend.setRealAmount(realAmount); // 平台收益
        lend.setStatus(LendStatusEnum.PAY_RUN.getStatus()); // 标的状态
        lend.setPaymentTime(LocalDateTime.now());
        baseMapper.updateById(lend);

        // 给借款账号转入金额
        // 获取借款人的bindCode
        Long userId = lend.getUserId();
        UserInfo userInfo = userInfoMapper.selectById(userId);
        String bindCode = userInfo.getBindCode();
        BigDecimal voteAmt = new BigDecimal(result.getString("voteAmt"));
        userAccountMapper.updateAccount(bindCode, voteAmt, new BigDecimal(0));

        // 增加借款交易流水
        TransFlowBO transFlowBO = new TransFlowBO(
                result.getString("agentBillNo"),
                bindCode,
                voteAmt,
                TransTypeEnum.BORROW_BACK,
                "开始放款，项目编号:" + lend.getLendNo() + ",项目名称:" + lend.getTitle());

        transFlowService.saveTransFlow(transFlowBO);

        // 解冻并扣除投资人资金
        // 获取标的下的投资列表
        List<LendItem> lendItemList = lendItemService.selectByLendId(id, 1);
        lendItemList.stream().forEach(item -> {
            Long investUserId = item.getInvestUserId();
            // 投资人
            UserInfo investUserInfo = userInfoMapper.selectById(investUserId);
            String investBindCode = investUserInfo.getBindCode();
            BigDecimal investAmount = item.getInvestAmount();
            userAccountMapper.updateAccount(investBindCode, new BigDecimal(0), investAmount.negate());
            // 增加投资人交易流水
            TransFlowBO investTransFlowBO = new TransFlowBO(
                    LendNoUtils.getTransNo(),
                    investBindCode,
                    investAmount,
                    TransTypeEnum.INVEST_UNLOCK,
                    "项目放款,冻结资金转出,项目编号:" + lend.getLendNo() + ",项目名称:" + lend.getTitle());

        });
        // 生成借款人还款计划和出借人回款计划
        this.repaymentPlan(lend);
    }

    /**
     * 还款计划
     *
     * @param lend
     */
    private void repaymentPlan(Lend lend) {

        //还款计划列表
        List<LendReturn> lendReturnList = new ArrayList<>();

        //按还款时间生成还款计划
        int len = lend.getPeriod();
        for (int i = 1; i <= len; i++) {

            //创建还款计划对象
            LendReturn lendReturn = new LendReturn();
            lendReturn.setReturnNo(LendNoUtils.getReturnNo());
            lendReturn.setLendId(lend.getId());
            lendReturn.setBorrowInfoId(lend.getBorrowInfoId());
            lendReturn.setUserId(lend.getUserId());
            lendReturn.setAmount(lend.getAmount());
            lendReturn.setBaseAmount(lend.getInvestAmount());
            lendReturn.setLendYearRate(lend.getLendYearRate());
            lendReturn.setCurrentPeriod(i);//当前期数
            lendReturn.setReturnMethod(lend.getReturnMethod());

            //说明：还款计划中的这三项 = 回款计划中对应的这三项和：因此需要先生成对应的回款计划
            //			lendReturn.setPrincipal();
            //			lendReturn.setInterest();
            //			lendReturn.setTotal();

            lendReturn.setFee(new BigDecimal(0));
            lendReturn.setReturnDate(lend.getLendStartDate().plusMonths(i)); //第二个月开始还款
            lendReturn.setOverdue(false);
            if (i == len) { //最后一个月
                //标识为最后一次还款
                lendReturn.setLast(true);
            } else {
                lendReturn.setLast(false);
            }
            lendReturn.setStatus(0);
            lendReturnList.add(lendReturn);
        }
        //批量保存
        lendReturnService.saveBatch(lendReturnList);

        //获取lendReturnList中还款期数与还款计划id对应map
        Map<Integer, Long> lendReturnMap = lendReturnList.stream().collect(
                Collectors.toMap(LendReturn::getCurrentPeriod, LendReturn::getId)
        );

        //======================================================
        //=============获取所有投资者，生成回款计划===================
        //======================================================
        //回款计划列表
        List<LendItemReturn> lendItemReturnAllList = new ArrayList<>();
        //获取投资成功的投资记录
        List<LendItem> lendItemList = lendItemService.selectByLendId(lend.getId(), 1);
        for (LendItem lendItem : lendItemList) {
            //创建回款计划列表
            List<LendItemReturn> lendItemReturnList = this.returnInvest(lendItem.getId(), lendReturnMap, lend);
            lendItemReturnAllList.addAll(lendItemReturnList);
        }

        //更新还款计划中的相关金额数据
        for (LendReturn lendReturn : lendReturnList) {
            BigDecimal sumPrincipal = lendItemReturnAllList.stream()
                    //过滤条件：当回款计划中的还款计划id == 当前还款计划id的时候
                    .filter(item -> item.getLendReturnId().longValue() == lendReturn.getId().longValue())
                    //将所有回款计划中计算的每月应收本金相加
                    .map(LendItemReturn::getPrincipal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sumInterest = lendItemReturnAllList.stream()
                    .filter(item -> item.getLendReturnId().longValue() == lendReturn.getId().longValue())
                    .map(LendItemReturn::getInterest)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sumTotal = lendItemReturnAllList.stream()
                    .filter(item -> item.getLendReturnId().longValue() == lendReturn.getId().longValue())
                    .map(LendItemReturn::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            lendReturn.setPrincipal(sumPrincipal); //每期还款本金
            lendReturn.setInterest(sumInterest); //每期还款利息
            lendReturn.setTotal(sumTotal); //每期还款本息
        }
        lendReturnService.updateBatchById(lendReturnList);
    }

    /**
     * 回款计划
     *
     * @param lendItemId
     * @param lendReturnMap
     * @param lend
     * @return
     */
    public List<LendItemReturn> returnInvest(Long lendItemId, Map<Integer, Long> lendReturnMap, Lend lend) {

        //投标信息
        LendItem lendItem = lendItemService.getById(lendItemId);

        //投资金额
        BigDecimal amount = lendItem.getInvestAmount();
        //年化利率
        BigDecimal yearRate = lendItem.getLendYearRate();
        //投资期数
        int totalMonth = lend.getPeriod();

        Map<Integer, BigDecimal> mapInterest = null;  //还款期数 -> 利息
        Map<Integer, BigDecimal> mapPrincipal = null; //还款期数 -> 本金

        //根据还款方式计算本金和利息
        if (lend.getReturnMethod().intValue() == ReturnMethodEnum.ONE.getMethod()) {
            //利息
            mapInterest = Amount1Helper.getPerMonthInterest(amount, yearRate, totalMonth);
            //本金
            mapPrincipal = Amount1Helper.getPerMonthPrincipal(amount, yearRate, totalMonth);
        } else if (lend.getReturnMethod().intValue() == ReturnMethodEnum.TWO.getMethod()) {
            mapInterest = Amount2Helper.getPerMonthInterest(amount, yearRate, totalMonth);
            mapPrincipal = Amount2Helper.getPerMonthPrincipal(amount, yearRate, totalMonth);
        } else if (lend.getReturnMethod().intValue() == ReturnMethodEnum.THREE.getMethod()) {
            mapInterest = Amount3Helper.getPerMonthInterest(amount, yearRate, totalMonth);
            mapPrincipal = Amount3Helper.getPerMonthPrincipal(amount, yearRate, totalMonth);
        } else {
            mapInterest = Amount4Helper.getPerMonthInterest(amount, yearRate, totalMonth);
            mapPrincipal = Amount4Helper.getPerMonthPrincipal(amount, yearRate, totalMonth);
        }

        //创建回款计划列表
        List<LendItemReturn> lendItemReturnList = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entry : mapInterest.entrySet()) {
            Integer currentPeriod = entry.getKey();
            //根据还款期数获取还款计划的id
            Long lendReturnId = lendReturnMap.get(currentPeriod);

            LendItemReturn lendItemReturn = new LendItemReturn();
            lendItemReturn.setLendReturnId(lendReturnId);
            lendItemReturn.setLendItemId(lendItemId);
            lendItemReturn.setInvestUserId(lendItem.getInvestUserId());
            lendItemReturn.setLendId(lendItem.getLendId());
            lendItemReturn.setInvestAmount(lendItem.getInvestAmount());
            lendItemReturn.setLendYearRate(lend.getLendYearRate());
            lendItemReturn.setCurrentPeriod(currentPeriod);
            lendItemReturn.setReturnMethod(lend.getReturnMethod());
            //最后一次本金计算
            if (lendItemReturnList.size() > 0 && currentPeriod.intValue() == lend.getPeriod().intValue()) {
                //最后一期本金 = 本金 - 前几次之和
                BigDecimal sumPrincipal = lendItemReturnList.stream()
                        .map(LendItemReturn::getPrincipal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                //最后一期应还本金 = 用当前投资人的总投资金额 - 除了最后一期前面期数计算出来的所有的应还本金
                BigDecimal lastPrincipal = lendItem.getInvestAmount().subtract(sumPrincipal);
                lendItemReturn.setPrincipal(lastPrincipal);

                // 利息
                BigDecimal sumInterest = lendItemReturnList.stream()
                        .map(LendItemReturn::getInterest)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal lastInterest = lendItem.getExpectAmount().subtract(sumInterest);
                lendItemReturn.setInterest(lastInterest);
            } else {
                lendItemReturn.setPrincipal(mapPrincipal.get(currentPeriod));
                lendItemReturn.setInterest(mapInterest.get(currentPeriod));
            }

            // 回款总金额
            lendItemReturn.setTotal(lendItemReturn.getPrincipal().add(lendItemReturn.getInterest()));
            lendItemReturn.setFee(new BigDecimal("0"));
            lendItemReturn.setReturnDate(lend.getLendStartDate().plusMonths(currentPeriod));
            //是否逾期，默认未逾期
            lendItemReturn.setOverdue(false);
            lendItemReturn.setStatus(0);

            lendItemReturnList.add(lendItemReturn);
        }
        lendItemReturnService.saveBatch(lendItemReturnList);

        return lendItemReturnList;
    }
}
