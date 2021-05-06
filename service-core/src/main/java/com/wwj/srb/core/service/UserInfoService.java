package com.wwj.srb.core.service;

import com.wwj.srb.core.pojo.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wwj.srb.core.pojo.vo.LoginVO;
import com.wwj.srb.core.pojo.vo.RegisterVO;
import com.wwj.srb.core.pojo.vo.UserInfoVO;

/**
 * <p>
 * 用户基本信息 服务类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
public interface UserInfoService extends IService<UserInfo> {

    /**
     * 完成用户注册
     *
     * @param registerVO 注册信息
     */
    void register(RegisterVO registerVO);

    /**
     * 实现用户登录
     *
     * @param loginVO 用户输入
     * @param ip      登录ip
     * @return 用户信息对象
     */
    UserInfoVO login(LoginVO loginVO, String ip);
}
