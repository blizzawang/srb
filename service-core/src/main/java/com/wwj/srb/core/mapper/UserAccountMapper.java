package com.wwj.srb.core.mapper;

import com.wwj.srb.core.pojo.entity.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * <p>
 * 用户账户 Mapper 接口
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    void updateAccount(@Param("bindCode") String bindCode,
                       @Param("amount") BigDecimal amount,
                       @Param("freezeAmount") BigDecimal freezeAmount);
}
