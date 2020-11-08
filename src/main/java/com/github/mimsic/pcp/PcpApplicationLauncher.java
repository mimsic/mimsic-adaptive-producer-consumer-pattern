package com.github.mimsic.pcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.github.mimsic.pcp"
})
public class PcpApplicationLauncher {

    public static void main(String[] args) {

        ApplicationContext ctx = SpringApplication.run(PcpApplicationLauncher.class, args);
    }
}
