package com.wwj.srb.core.controller.admin;


import com.wwj.common.result.R;
import com.wwj.common.result.ResponseEnum;
import com.wwj.common.result.exception.BusinessException;
import com.wwj.srb.core.service.DictService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 数据字典 前端控制器
 * </p>
 *
 * @author wangweijun
 * @since 2021-04-27
 */
@Api(tags = "数据字典管理")
@RestController
@RequestMapping("/admin/core/dict")
@Slf4j
@CrossOrigin
public class AdminDictController {

    @Autowired
    private DictService dictService;

    @ApiOperation("批量导入Excel数据")
    @PostMapping("/import")
    public R batchImport(
            @ApiParam(value = "Excel数据字典文件", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            dictService.importData(file.getInputStream());
            return R.ok().message("数据批量导入成功");
        } catch (Exception e) {
            throw new BusinessException(ResponseEnum.UPLOAD_ERROR, e);
        }
    }
}

