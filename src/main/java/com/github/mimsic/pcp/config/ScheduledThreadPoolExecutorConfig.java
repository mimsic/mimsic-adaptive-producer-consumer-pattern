package com.github.mimsic.pcp.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "scheduled-thread-pool-executor-config")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledThreadPoolExecutorConfig implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledThreadPoolExecutorConfig.class);

    private TimeUnit timeUnit;
    private long awaitTermination;
    private int corePoolSize;
    private boolean cancelPolicy;
    private boolean prestart;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info(this.toString());
    }

    @Override
    public String toString() {

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Scheduled Thread Pool Executor Config: {").append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Core Pool Size: ").append(corePoolSize).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Await Termination: ").append(awaitTermination).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Cancel Policy: ").append(cancelPolicy).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Prestart All Core Threads: ").append(prestart).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Time Unit: ").append(timeUnit).append('\n');
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
