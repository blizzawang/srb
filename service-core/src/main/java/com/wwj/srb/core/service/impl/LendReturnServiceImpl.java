package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.srb.core.mapper.LendReturnMapper;
import com.wwj.srb.core.pojo.entity.LendReturn;
import com.wwj.srb.core.service.LendReturnService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 还款记录表 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
public class LendReturnServiceImpl extends ServiceImpl<LendReturnMapper, LendReturn> implements LendReturnService {

    @Override
    public List<LendReturn> selectByLendId(Long lendId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<LendReturn>()
                        .eq(LendReturn::getLendId, lendId));
    }
}
