package com.wwj.srb.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.srb.core.enums.BorrowerStatusEnum;
import com.wwj.srb.core.mapper.BorrowerAttachMapper;
import com.wwj.srb.core.mapper.BorrowerMapper;
import com.wwj.srb.core.mapper.UserInfoMapper;
import com.wwj.srb.core.pojo.entity.Borrower;
import com.wwj.srb.core.pojo.entity.BorrowerAttach;
import com.wwj.srb.core.pojo.entity.UserInfo;
import com.wwj.srb.core.pojo.vo.BorrowerVO;
import com.wwj.srb.core.service.BorrowerService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 借款人 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
public class BorrowerServiceImpl extends ServiceImpl<BorrowerMapper, Borrower> implements BorrowerService {

    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private BorrowerAttachMapper borrowerAttachMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveBorrowerVOByUserId(BorrowerVO borrowerVO, Long userId) {
        // 获取用户基本信息
        UserInfo userInfo = userInfoMapper.selectById(userId);

        // 保存借款人信息
        Borrower borrower = new Borrower();
        BeanUtils.copyProperties(borrowerVO, borrower);

        borrower.setUserId(userId);
        borrower.setName(userInfo.getName());
        borrower.setIdCard(userInfo.getIdCard());
        borrower.setMobile(userInfo.getMobile());
        // 设置状态为认证中
        borrower.setStatus(BorrowerStatusEnum.AUTH_RUN.getStatus());
        baseMapper.insert(borrower);

        // 保存附件
        List<BorrowerAttach> borrowerAttachList = borrowerVO.getBorrowerAttachList();
        borrowerAttachList.forEach(borrowerAttach -> {
            borrowerAttach.setBorrowerId(borrower.getId());
            borrowerAttachMapper.insert(borrowerAttach);
        });

        // 更新user_info表中的借款人认证状态
        userInfo.setBorrowAuthStatus(BorrowerStatusEnum.AUTH_RUN.getStatus());
        userInfoMapper.updateById(userInfo);
    }

    @Override
    public Integer getStatusByUserId(Long userId) {
        List<Object> statusList = baseMapper.selectObjs(
                new LambdaQueryWrapper<Borrower>()
                        .select(Borrower::getStatus)
                        .eq(Borrower::getUserId, userId));
        if (statusList.size() == 0) {
            return BorrowerStatusEnum.NO_AUTH.getStatus();
        }
        return (Integer) statusList.get(0);
    }
}
