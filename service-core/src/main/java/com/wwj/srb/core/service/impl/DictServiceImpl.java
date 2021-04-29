package com.wwj.srb.core.service.impl;

import com.alibaba.excel.EasyExcel;
import com.wwj.srb.core.listener.ExcelDictDTOListener;
import com.wwj.srb.core.pojo.dto.ExcelDictDTO;
import com.wwj.srb.core.pojo.entity.Dict;
import com.wwj.srb.core.mapper.DictMapper;
import com.wwj.srb.core.service.DictService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

/**
 * <p>
 * 数据字典 服务实现类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Service
@Slf4j
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    @Autowired
    private DictMapper dictMapper;

    @Transactional(rollbackFor = Exception.class) // 当出现异常时，进行回滚
    @Override
    public void importData(InputStream inputStream) {
        // 读取Excel
        EasyExcel.read(inputStream, ExcelDictDTO.class, new ExcelDictDTOListener(dictMapper)).sheet().doRead();
        log.info("Excel导入成功!");
    }
}
