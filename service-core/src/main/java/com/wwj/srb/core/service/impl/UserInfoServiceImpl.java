package com.wwj.srb.core.service.impl;

import com.wwj.srb.core.pojo.entity.UserInfo;
import com.wwj.srb.core.mapper.UserInfoMapper;
import com.wwj.srb.core.service.UserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户基本信息 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

}
