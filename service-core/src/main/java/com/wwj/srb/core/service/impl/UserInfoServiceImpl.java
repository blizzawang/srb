package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.common.result.ResponseEnum;
import com.wwj.common.result.exception.Assert;
import com.wwj.common.util.MD5;
import com.wwj.srb.base.util.JwtUtils;
import com.wwj.srb.core.mapper.UserAccountMapper;
import com.wwj.srb.core.mapper.UserInfoMapper;
import com.wwj.srb.core.mapper.UserLoginRecordMapper;
import com.wwj.srb.core.pojo.entity.UserAccount;
import com.wwj.srb.core.pojo.entity.UserInfo;
import com.wwj.srb.core.pojo.entity.UserLoginRecord;
import com.wwj.srb.core.pojo.query.UserInfoQuery;
import com.wwj.srb.core.pojo.vo.LoginVO;
import com.wwj.srb.core.pojo.vo.RegisterVO;
import com.wwj.srb.core.pojo.vo.UserIndexVO;
import com.wwj.srb.core.pojo.vo.UserInfoVO;
import com.wwj.srb.core.service.UserInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

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

    @Resource
    private UserAccountMapper userAccountMapper;
    @Resource
    private UserLoginRecordMapper userLoginRecordMapper;

    /**
     * 完成用户注册
     *
     * @param registerVO 注册信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(RegisterVO registerVO) {
        // 判断当前手机号是否已经被注册
        Integer count = baseMapper.selectCount(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getMobile, registerVO.getMobile()));
        Assert.isTrue(count == 0, ResponseEnum.MOBILE_EXIST_ERROR);

        // 插入用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setUserType(registerVO.getUserType());
        userInfo.setNickName(registerVO.getMobile());
        userInfo.setName(registerVO.getMobile());
        userInfo.setMobile(registerVO.getMobile());
        userInfo.setPassword(MD5.encrypt(registerVO.getPassword()));
        userInfo.setStatus(UserInfo.STATUS_NORMAL);
        userInfo.setHeadImg(UserInfo.USER_AVATAR);
        baseMapper.insert(userInfo);

        // 插入用户账户信息
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userInfo.getId());
        userAccountMapper.insert(userAccount);
    }

    /**
     * 实现用户登录
     *
     * @param loginVO 用户输入
     * @param ip      登录ip
     * @return 用户信息对象
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserInfoVO login(LoginVO loginVO, String ip) {
        String mobile = loginVO.getMobile();
        String password = loginVO.getPassword();
        Integer userType = loginVO.getUserType();

        // 判断用户是否存在
        UserInfo userInfo = baseMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getMobile, mobile)
                        .eq(UserInfo::getUserType, userType));
        Assert.notNull(userInfo, ResponseEnum.LOGIN_MOBILE_ERROR);

        // 判断密码是否正确
        Assert.equals(MD5.encrypt(password), userInfo.getPassword(), ResponseEnum.LOGIN_PASSWORD_ERROR);

        // 判断用户是否被禁用
        Assert.equals(userInfo.getStatus(), UserInfo.STATUS_NORMAL, ResponseEnum.LOGIN_LOKED_ERROR);

        // 记录登录日志
        UserLoginRecord userLoginRecord = new UserLoginRecord();
        userLoginRecord.setUserId(userInfo.getId());
        userLoginRecord.setIp(ip);
        userLoginRecordMapper.insert(userLoginRecord);

        // 生成token
        String token = JwtUtils.createToken(userInfo.getId(), userInfo.getName());

        // 封装UserInfoVO
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setToken(token);
        userInfoVO.setName(userInfo.getName());
        userInfoVO.setNickName(userInfo.getNickName());
        userInfoVO.setHeadImg(userInfo.getHeadImg());
        userInfoVO.setMobile(mobile);
        userInfoVO.setUserType(userType);
        return userInfoVO;
    }

    @Override
    public IPage<UserInfo> listPage(Page<UserInfo> pageParam, UserInfoQuery userInfoQuery) {
        if (userInfoQuery == null) {
            return baseMapper.selectPage(pageParam, null);
        }
        String mobile = userInfoQuery.getMobile();
        String status = userInfoQuery.getStatus();
        String userType = userInfoQuery.getUserType();

        // 封装查询条件
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<UserInfo>()
                .eq(StringUtils.isNotBlank(mobile), UserInfo::getMobile, mobile)
                .eq(status != null && !status.equals(""), UserInfo::getStatus, status)
                .eq(userType != null && !userType.equals(""), UserInfo::getUserType, userType);

        return baseMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public void lock(Long id, Integer status) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(id);
        userInfo.setStatus(status);
        baseMapper.updateById(userInfo);
    }

    @Override
    public boolean checkMobile(String mobile) {
        Integer count = baseMapper.selectCount(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getMobile, mobile)
        );
        return count > 0;
    }

    @Override
    public UserIndexVO getIndexUserInfo(Long userId) {
        //用户信息
        UserInfo userInfo = baseMapper.selectById(userId);

        //账户信息
        UserAccount userAccount = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getUserId, userId));
        //登录信息
        UserLoginRecord userLoginRecord = userLoginRecordMapper.selectOne(
                new LambdaQueryWrapper<UserLoginRecord>()
                        .eq(UserLoginRecord::getUserId, userId)
                        .orderByDesc(UserLoginRecord::getId)
                        .last("limit 1"));

        //组装结果数据
        UserIndexVO userIndexVO = new UserIndexVO();
        userIndexVO.setUserId(userInfo.getId());
        userIndexVO.setUserType(userInfo.getUserType());
        userIndexVO.setName(userInfo.getName());
        userIndexVO.setNickName(userInfo.getNickName());
        userIndexVO.setHeadImg(userInfo.getHeadImg());
        userIndexVO.setBindStatus(userInfo.getBindStatus());
        userIndexVO.setAmount(userAccount.getAmount());
        userIndexVO.setFreezeAmount(userAccount.getFreezeAmount());
        userIndexVO.setLastLoginTime(userLoginRecord.getCreateTime());

        return userIndexVO;
    }

    @Override
    public String getMobileByBindCode(String bindCode) {
        UserInfo userInfo = baseMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getBindCode, bindCode));
        return userInfo.getMobile();
    }
}
