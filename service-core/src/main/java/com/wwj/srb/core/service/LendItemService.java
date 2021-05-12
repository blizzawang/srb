package com.wwj.srb.core.service;

import com.wwj.srb.core.pojo.entity.LendItem;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wwj.srb.core.pojo.vo.InvestVO;

import java.util.Map;

/**
 * <p>
 * 标的出借记录表 服务类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
public interface LendItemService extends IService<LendItem> {

    String commitInvest(InvestVO investVO);

    void notify(Map<String, Object> paramMap);
}
