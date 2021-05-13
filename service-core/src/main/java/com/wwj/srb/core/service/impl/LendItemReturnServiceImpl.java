package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.srb.core.mapper.LendItemReturnMapper;
import com.wwj.srb.core.pojo.entity.LendItemReturn;
import com.wwj.srb.core.service.LendItemReturnService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 标的出借回款记录表 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
public class LendItemReturnServiceImpl extends ServiceImpl<LendItemReturnMapper, LendItemReturn> implements LendItemReturnService {

    @Override
    public List<LendItemReturn> selectByLendId(Long lendId, Long userId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<LendItemReturn>()
                        .eq(LendItemReturn::getLendId, lendId)
                        .eq(LendItemReturn::getInvestUserId, userId)
                        .orderByAsc(LendItemReturn::getCurrentPeriod));
    }
}
