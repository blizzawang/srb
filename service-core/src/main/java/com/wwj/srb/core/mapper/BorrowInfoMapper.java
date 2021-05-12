package com.wwj.srb.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwj.srb.core.pojo.entity.BorrowInfo;

import java.util.List;

/**
 * <p>
 * 借款信息表 Mapper 接口
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
public interface BorrowInfoMapper extends BaseMapper<BorrowInfo> {

    List<BorrowInfo> selectBorrowInfoList();
}
