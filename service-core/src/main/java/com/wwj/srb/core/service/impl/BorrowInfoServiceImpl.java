package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.common.result.ResponseEnum;
import com.wwj.common.result.exception.Assert;
import com.wwj.srb.core.enums.BorrowInfoStatusEnum;
import com.wwj.srb.core.enums.BorrowerStatusEnum;
import com.wwj.srb.core.enums.UserBindEnum;
import com.wwj.srb.core.mapper.BorrowInfoMapper;
import com.wwj.srb.core.mapper.IntegralGradeMapper;
import com.wwj.srb.core.mapper.UserInfoMapper;
import com.wwj.srb.core.pojo.entity.BorrowInfo;
import com.wwj.srb.core.pojo.entity.IntegralGrade;
import com.wwj.srb.core.pojo.entity.UserInfo;
import com.wwj.srb.core.service.BorrowInfoService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * <p>
 * 借款信息表 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
public class BorrowInfoServiceImpl extends ServiceImpl<BorrowInfoMapper, BorrowInfo> implements BorrowInfoService {

    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private IntegralGradeMapper integralGradeMapper;

    @Override
    public BigDecimal getBorrowAmount(Long userId) {
        // 获取用户积分
        UserInfo userInfo = userInfoMapper.selectById(userId);
        Assert.notNull(userInfo, ResponseEnum.LOGIN_MOBILE_ERROR);
        Integer integral = userInfo.getIntegral();
        // 根据积分查询借款额度
        IntegralGrade integralGrade = integralGradeMapper.selectOne(
                new LambdaQueryWrapper<IntegralGrade>()
                        .le(IntegralGrade::getIntegralStart, integral)
                        .ge(IntegralGrade::getIntegralEnd, integral));
        if (integralGrade == null) {
            return new BigDecimal("0");
        }
        return integralGrade.getBorrowAmount();
    }

    @Override
    public void saveBorrowInfo(BorrowInfo borrowInfo, Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        // 判断用户绑定状态
        Assert.isTrue(userInfo.getBindStatus().intValue() == UserBindEnum.BIND_OK.getStatus().intValue(), ResponseEnum.USER_NO_BIND_ERROR);
        // 判断借款额度申请状态
        Assert.isTrue(userInfo.getBorrowAuthStatus().intValue() == BorrowerStatusEnum.AUTH_OK.getStatus().intValue(), ResponseEnum.USER_NO_AMOUNT_ERROR);
        // 判断借款额度是否充足
        BigDecimal borrowAmount = this.getBorrowAmount(userId);
        Assert.isTrue(borrowInfo.getAmount().doubleValue() <= borrowAmount.doubleValue(), ResponseEnum.USER_AMOUNT_LESS_ERROR);
        // 存储borrowInfo到数据表
        borrowInfo.setUserId(userId);
        // 转换一下年利率
        BigDecimal borrowYearRate = borrowInfo.getBorrowYearRate().divide(new BigDecimal("100"));
        borrowInfo.setBorrowYearRate(borrowYearRate);
        // 设置状态为审核中
        borrowInfo.setStatus(BorrowInfoStatusEnum.CHECK_RUN.getStatus());
        baseMapper.insert(borrowInfo);
    }
}
