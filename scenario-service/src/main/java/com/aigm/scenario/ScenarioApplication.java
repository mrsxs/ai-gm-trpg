package com.aigm.scenario;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** scenario-service（端口 8082）：剧本/节点/NPC/分支 CRUD + 状态机建模 + 运行时只读接口。 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.aigm.scenario.mapper")
public class ScenarioApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScenarioApplication.class, args);
    }
}
