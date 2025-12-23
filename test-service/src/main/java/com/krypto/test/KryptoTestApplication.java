package com.krypto.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(
        scanBasePackages = {
                "com.krypto.test",
                "com.krypto.common"
        }
)
@EnableDiscoveryClient
public class KryptoTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(KryptoTestApplication.class, args);
    }

}
