package com.wwj.srb.core.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.wwj.srb.core.mapper.DictMapper;
import com.wwj.srb.core.pojo.dto.ExcelDictDTO;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成Excel的监听器
 */
@Slf4j
@NoArgsConstructor
public class ExcelDictDTOListener extends AnalysisEventListener<ExcelDictDTO> {

    private DictMapper dictMapper;

    /**
     * 数据列表，用于临时存放Excel数据，当数据达到一定量时，向数据库插入数据
     */
    private List<ExcelDictDTO> list = new ArrayList<>();

    /**
     * 每五条数据执行一次插入
     */
    private static final int BATCH_COUNT = 5;

    /**
     * 通过构造函数注入Mapper接口
     *
     * @param dictMapper
     */
    public ExcelDictDTOListener(DictMapper dictMapper) {
        this.dictMapper = dictMapper;
    }

    /**
     * 解析每条记录都会调用此方法
     *
     * @param data
     * @param analysisContext
     */
    @Override
    public void invoke(ExcelDictDTO data, AnalysisContext analysisContext) {
        log.info("解析到一条记录:{}", data);
        // 将数据存入数据列表
        list.add(data);
        if (list.size() >= BATCH_COUNT) {
            // 保存数据
            saveData();
            // 清空数据列表
            list.clear();
        }
    }

    private void saveData() {
        log.info("{}条数据被存储到数据库", list.size());
        // 调用Mapper层的批量保存方法
        dictMapper.insertBatch(list);
        log.info("{}条数据存储成功", list.size());
    }

    /**
     * 解析结束后调用此方法
     *
     * @param analysisContext
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // 处理剩下的几条记录
        saveData();
        log.info("解析完成");
    }
}
