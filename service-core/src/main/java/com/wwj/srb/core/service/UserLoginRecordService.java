package com.wwj.srb.core.service;

import com.wwj.srb.core.pojo.entity.UserLoginRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 用户登录记录表 服务类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
public interface UserLoginRecordService extends IService<UserLoginRecord> {

    /**
     * 查询前50条用户日志
     *
     * @param userId 用户id
     * @return 日志列表
     */
    List<UserLoginRecord> listTop50(Long userId);
}
