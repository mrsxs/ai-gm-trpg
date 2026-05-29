package com.aigm.memory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** memory-service（端口 8085，INTERNAL）：pgvector 长程记忆 store/recall RAG。 */
@SpringBootApplication
@EnableDiscoveryClient
public class MemoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(MemoryApplication.class, args);
    }
}
