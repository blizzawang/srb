package com.wwj.common.result;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一返回结果
 */
@Data
public class R {

    private Integer code;
    private String message;
    private Map<String, Object> data = new HashMap<>();

    private R() {
    }

    /**
     * 返回成功结果
     *
     * @return
     */
    public static R ok() {
        R r = new R();
        r.setCode(ResponseEnum.SUCCESS.getCode());
        r.setMessage(ResponseEnum.SUCCESS.getMessage());
        return r;
    }

    /**
     * 返回失败结果
     *
     * @return
     */
    public static R error() {
        R r = new R();
        r.setCode(ResponseEnum.ERROR.getCode());
        r.setMessage(ResponseEnum.ERROR.getMessage());
        return r;
    }

    /**
     * 返回特定结果
     *
     * @param responseEnum
     * @return
     */
    public static R setResult(ResponseEnum responseEnum) {
        R r = new R();
        r.setCode(responseEnum.getCode());
        r.setMessage(responseEnum.getMessage());
        return r;
    }

    /**
     * 封装返回数据
     *
     * @param key
     * @param value
     * @return
     */
    public R data(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * 封装返回数据，若为Map集合，则直接赋值即可
     *
     * @param map
     * @return
     */
    public R data(Map<String, Object> map) {
        this.setData(map);
        return this;
    }

    /**
     * 设置特定的消息
     *
     * @param message
     * @return
     */
    public R message(String message) {
        this.setMessage(message);
        return this;
    }

    /**
     * 设置特定的响应码
     *
     * @param code
     * @return
     */
    public R code(Integer code) {
        this.setCode(code);
        return this;
    }
}
