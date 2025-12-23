package com.kyojin.krypto.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(
        scanBasePackages = {
                "com.kyojin.krypto.test",
                "com.kyojin.krypto.common"
        }
)
@EnableDiscoveryClient
public class KryptoTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(KryptoTestApplication.class, args);
    }

}
