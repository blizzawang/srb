package com.wwj.srb.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwj.srb.core.pojo.dto.ExcelDictDTO;
import com.wwj.srb.core.pojo.entity.Dict;

import java.util.List;

/**
 * <p>
 * 数据字典 Mapper 接口
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
public interface DictMapper extends BaseMapper<Dict> {

    void insertBatch(List<ExcelDictDTO> list);
}
