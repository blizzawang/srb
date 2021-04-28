package com.wwj.srb.base.config;

import com.google.common.base.Predicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * 对两套Api进行分组管理，/admin为服务后台接口，/api为服务前端接口
 */
@Configuration
@EnableSwagger2
public class Swagger2Config {

    /**
     * 生成接口文档
     *
     * @return
     */
    @Bean
    public Docket adminApiConfig() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("adminApi")
                .apiInfo(adminApiInfo())
                .select()
                .paths(Predicates.and(PathSelectors.regex("/admin/.*")))
                .build();
    }

    /**
     * 生成接口文档
     *
     * @return
     */
    @Bean
    public Docket webApiConfig() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("webApi")
                .apiInfo(webApiInfo())
                .select()
                .paths(Predicates.and(PathSelectors.regex("/api/.*")))
                .build();
    }

    /**
     * 封装服务后台接口文档信息
     *
     * @return
     */
    private ApiInfo adminApiInfo() {
        return new ApiInfoBuilder()
                .title("尚融宝后台管理系统API文档")
                .description("本文档描述了尚融宝后台管理系统各个模块的接口调用方式")
                .version("1.0")
                .contact(new Contact("wangweijun", null, "blizzawang@163.com"))
                .build();
    }

    /**
     * 封装服务前端接口文档信息
     *
     * @return
     */
    private ApiInfo webApiInfo() {
        return new ApiInfoBuilder()
                .title("尚融宝前端API文档")
                .description("本文档描述了尚融宝前端系统的接口调用方式")
                .version("1.0")
                .contact(new Contact("wangweijun", null, "blizzawang@163.com"))
                .build();
    }
}
