package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.common.result.ResponseEnum;
import com.wwj.common.result.exception.Assert;
import com.wwj.srb.core.enums.UserBindEnum;
import com.wwj.srb.core.hfb.FormHelper;
import com.wwj.srb.core.hfb.HfbConst;
import com.wwj.srb.core.hfb.RequestHelper;
import com.wwj.srb.core.mapper.UserBindMapper;
import com.wwj.srb.core.mapper.UserInfoMapper;
import com.wwj.srb.core.pojo.entity.UserBind;
import com.wwj.srb.core.pojo.entity.UserInfo;
import com.wwj.srb.core.pojo.vo.UserBindVO;
import com.wwj.srb.core.service.UserBindService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户绑定表 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
public class UserBindServiceImpl extends ServiceImpl<UserBindMapper, UserBind> implements UserBindService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Override
    public String commitBindUser(UserBindVO userBindVO, Long userId) {
        // 当不同的用户使用同一个身份证进行绑定时，是不被允许的
        UserBind userBind = baseMapper.selectOne(
                new LambdaQueryWrapper<UserBind>()
                        .eq(UserBind::getIdCard, userBindVO.getIdCard())
                        .ne(UserBind::getUserId, userId));
        Assert.isNull(userBind, ResponseEnum.USER_BIND_IDCARD_EXIST_ERROR);

        // 校验用户是否已经绑定（用户点击开户后，绑定信息会被存入数据表，但此时用户并没有正常执行接下来的流程
        // 当用户下次进行绑定时，就不能将该用户作为一个新的用户进行绑定）
        userBind = baseMapper.selectOne(
                new LambdaQueryWrapper<UserBind>()
                        .eq(UserBind::getUserId, userId));
        if (userBind == null) {
            // 若是当前用户不存在于绑定表，则正常创建绑定信息
            userBind = new UserBind();
            // 属性拷贝
            BeanUtils.copyProperties(userBindVO, userBind);
            userBind.setUserId(userId);
            // 初始状态设置为未绑定
            userBind.setStatus(UserBindEnum.NO_BIND.getStatus());
            // 存储用户的绑定信息
            baseMapper.insert(userBind);
        } else {
            // 若是当前用户存在，则更新当前用户的绑定信息
            BeanUtils.copyProperties(userBindVO, userBind);
            baseMapper.updateById(userBind);
        }

        // 封装汇付宝接口需要的参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentUserId", userId);
        paramMap.put("idCard", userBindVO.getIdCard());
        paramMap.put("personalName", userBindVO.getName());
        paramMap.put("bankType", userBindVO.getBankType());
        paramMap.put("bankNo", userBindVO.getBankNo());
        paramMap.put("mobile", userBindVO.getMobile());

        paramMap.put("returnUrl", HfbConst.USERBIND_RETURN_URL);
        paramMap.put("notifyUrl", HfbConst.USERBIND_NOTIFY_URL);

        paramMap.put("timestamp", RequestHelper.getTimestamp());
        paramMap.put("sign", RequestHelper.getSign(paramMap));

        // 生成动态表单字符串
        return FormHelper.buildForm(HfbConst.USERBIND_URL, paramMap);
    }

    @Override
    public void notify(Map<String, Object> paramMap) {
        String bindCode = (String) paramMap.get("bindCode");
        String agentUserId = (String) paramMap.get("agentUserId");

        // 根据userId查询userBind
        UserBind userBind = baseMapper.selectOne(
                new LambdaQueryWrapper<UserBind>()
                        .eq(UserBind::getUserId, agentUserId));
        userBind.setBindCode(bindCode);
        // 绑定状态设置为已绑定
        userBind.setStatus(UserBindEnum.BIND_OK.getStatus());
        // 更新用户绑定表
        baseMapper.updateById(userBind);

        // 更新用户表
        UserInfo userInfo = userInfoMapper.selectById(agentUserId);
        userInfo.setBindCode(bindCode);
        userInfo.setName(userBind.getName());
        userInfo.setIdCard(userBind.getIdCard());
        userInfo.setBindStatus(UserBindEnum.BIND_OK.getStatus());
        userInfoMapper.updateById(userInfo);
    }
}
