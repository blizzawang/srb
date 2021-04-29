package com.wwj.srb.core.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwj.srb.core.listener.ExcelDictDTOListener;
import com.wwj.srb.core.mapper.DictMapper;
import com.wwj.srb.core.pojo.dto.ExcelDictDTO;
import com.wwj.srb.core.pojo.entity.Dict;
import com.wwj.srb.core.service.DictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public List<ExcelDictDTO> listDictData() {
        List<Dict> dictList = baseMapper.selectList(null);
        // 将DictList收集为ExcelDictDTOList
        List<ExcelDictDTO> excelDictDTOList = new ArrayList<>();
        dictList.forEach(dict -> {
            ExcelDictDTO excelDictDTO = new ExcelDictDTO();
            // 属性拷贝
            BeanUtils.copyProperties(dict, excelDictDTO);
            excelDictDTOList.add(excelDictDTO);
        });
        return excelDictDTOList;
    }

    @Override
    public List<Dict> listByParentId(Long parentId) {
        List<Dict> list = baseMapper.selectList(
                new LambdaQueryWrapper<Dict>()
                        .eq(Dict::getParentId, parentId));
        // 填充hasChildren属性
        List<Dict> dictList = list.stream().map(dict -> {
            // 判断当前节点是否有子节点，找到当前dict的下级有没有子节点
            boolean hasChildren = hasChildren(dict.getId());
            dict.setHasChildren(hasChildren);
            return dict;
        }).collect(Collectors.toList());
        return dictList;
    }

    /**
     * 判断当前id所在的节点下是否有子节点
     *
     * @param id
     * @return
     */
    private boolean hasChildren(Long id) {
        Integer count = dictMapper.selectCount(
                new LambdaQueryWrapper<Dict>()
                        .eq(Dict::getParentId, id));
        return count > 0;
    }
}
