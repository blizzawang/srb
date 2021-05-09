package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wwj.srb.core.pojo.entity.UserLoginRecord;
import com.wwj.srb.core.mapper.UserLoginRecordMapper;
import com.wwj.srb.core.service.UserLoginRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 用户登录记录表 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
public class UserLoginRecordServiceImpl extends ServiceImpl<UserLoginRecordMapper, UserLoginRecord> implements UserLoginRecordService {

    @Override
    public List<UserLoginRecord> listTop50(Long userId) {
        List<UserLoginRecord> list = baseMapper.selectList(
                new LambdaQueryWrapper<UserLoginRecord>()
                        .eq(UserLoginRecord::getUserId, userId)
                        .orderByDesc(UserLoginRecord::getId)
                        .last("limit 50"));
        return list;
    }
}
