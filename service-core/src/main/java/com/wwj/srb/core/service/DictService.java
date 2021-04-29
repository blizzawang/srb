package com.wwj.srb.core.service;

import com.wwj.srb.core.pojo.entity.Dict;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.InputStream;

/**
 * <p>
 * 数据字典 服务类
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
public interface DictService extends IService<Dict> {

    /**
     * 获取前端选择的Excel文件流，将其保存至数据库
     * @param inputStream
     */
    void importData(InputStream inputStream);
}
