package com.wwj.srb.oss.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CannedAccessControlList;
import com.wwj.srb.oss.service.FileService;
import com.wwj.srb.oss.util.OssProperties;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    /**
     * 文件上传至阿里云
     *
     * @param inputStream 文件流
     * @param module      文件类型
     * @param fileName    文件名
     * @return 文件的url地址
     */
    @Override
    public String upload(InputStream inputStream, String module, String fileName) {
        // 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(
                OssProperties.ENDPOINT, OssProperties.KEY_ID, OssProperties.KEY_SECRET);
        // 判断BUCKET_NAME是否存在
        if (!ossClient.doesBucketExist(OssProperties.BUCKET_NAME)) {
            // 若不存在，则创建
            ossClient.createBucket(OssProperties.BUCKET_NAME);
            // 设置访问权限为读写
            ossClient.setBucketAcl(OssProperties.BUCKET_NAME, CannedAccessControlList.PublicRead);
        }
        // 上传文件流
        // 创建文件目录结构
        String timeFolder = new DateTime().toString("/yyyy/MM/dd/");
        // 生成文件名
        fileName = UUID.randomUUID() + fileName.substring(fileName.lastIndexOf("."));
        String key = module + timeFolder + fileName;
        ossClient.putObject(OssProperties.BUCKET_NAME, key, inputStream);
        // 关闭OSSClient
        ossClient.shutdown();
        // 返回文件的url地址
        return "https://" + OssProperties.BUCKET_NAME + "." + OssProperties.ENDPOINT + "/" + key;
    }

    /**
     * 删除oss文件
     *
     * @param url 文件url地址
     */
    @Override
    public void removeFile(String url) {
        // 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(
                OssProperties.ENDPOINT, OssProperties.KEY_ID, OssProperties.KEY_SECRET);
        // 文件原url：https://srb-file-wwj.oss-cn-beijing.aliyuncs.com/test/2021/05/05c8c9f2ed-3f01-4a31-a38b-6ceeda2e97a5.png
        // objectName：test/2021/05/05c8c9f2ed-3f01-4a31-a38b-6ceeda2e97a5.png
        String host = "https://" + OssProperties.BUCKET_NAME + "." + OssProperties.ENDPOINT + "/";
        // 截掉原url前面的部分字符串
        String objectName = url.substring(host.length());
        // 删除文件
        ossClient.deleteObject(OssProperties.BUCKET_NAME, objectName);
        // 关闭OSSClient
        ossClient.shutdown();
    }
}
