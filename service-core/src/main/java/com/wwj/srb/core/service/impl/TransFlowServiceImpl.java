package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.srb.core.mapper.TransFlowMapper;
import com.wwj.srb.core.mapper.UserInfoMapper;
import com.wwj.srb.core.pojo.bo.TransFlowBO;
import com.wwj.srb.core.pojo.entity.TransFlow;
import com.wwj.srb.core.pojo.entity.UserInfo;
import com.wwj.srb.core.service.TransFlowService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 交易流水表 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
public class TransFlowServiceImpl extends ServiceImpl<TransFlowMapper, TransFlow> implements TransFlowService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Override
    public void saveTransFlow(TransFlowBO transFlowBO) {
        // 查询userInfo
        String bindCode = transFlowBO.getBindCode();
        UserInfo userInfo = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getBindCode, bindCode));

        TransFlow transFlow = new TransFlow();
        transFlow.setTransAmount(transFlowBO.getAmount());
        transFlow.setMemo(transFlowBO.getMemo());
        transFlow.setTransTypeName(transFlowBO.getTransTypeEnum().getTransTypeName());
        transFlow.setTransType(transFlowBO.getTransTypeEnum().getTransType());
        transFlow.setTransNo(transFlowBO.getAgentBillNo()); // 流水号
        transFlow.setUserId(userInfo.getId());
        transFlow.setUserName(userInfo.getName());
        // 保存交易流水
        baseMapper.insert(transFlow);
    }

    @Override
    public boolean isSaveTransFlow(String agentBillNo) {
        Integer count = baseMapper.selectCount(
                new LambdaQueryWrapper<TransFlow>()
                        .eq(TransFlow::getTransNo, agentBillNo));
        return count > 0;
    }

    @Override
    public List<TransFlow> selectByUserId(Long userId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<TransFlow>()
                        .eq(TransFlow::getUserId, userId)
                        .orderByDesc(TransFlow::getId));
    }
}
