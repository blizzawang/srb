package com.wwj.srb.oss.service;

import java.io.InputStream;

public interface FileService {

    /**
     * 文件上传至阿里云
     *
     * @param inputStream 文件流
     * @param module      文件类型
     * @param fileName    文件名
     * @return 文件的url地址
     */
    String upload(InputStream inputStream, String module, String fileName);

    /**
     * 删除oss文件
     *
     * @param url 文件url地址
     */
    void removeFile(String url);
}
