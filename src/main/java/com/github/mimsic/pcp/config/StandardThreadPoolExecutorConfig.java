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
@ConfigurationProperties(prefix = "standard-thread-pool-executor-config")
@Getter
@Setter
@NoArgsConstructor
public class StandardThreadPoolExecutorConfig implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardThreadPoolExecutorConfig.class);

    private TimeUnit timeUnit;
    private long awaitTermination;
    private long keepAliveTime;
    private long resubmissionDelay;
    private int corePoolSize;
    private int maxPoolSize;
    private int queueCapacity;
    private boolean prestart;
    private boolean timeOut;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info(this.toString());
    }

    @Override
    public String toString() {

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Standard Thread Pool Executor Config: {").append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Core Pool Size: ").append(corePoolSize).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Maximum Pool Size: ").append(maxPoolSize).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Queue Capacity: ").append(queueCapacity).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Await Termination: ").append(awaitTermination).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Keep Alive Time: ").append(keepAliveTime).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Resubmission Delay: ").append(resubmissionDelay).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Prestart All Core Threads: ").append(prestart).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Allow Core Thread Time Out: ").append(timeOut).append('\n');
        stringBuilder.append(StringUtils.leftPad("", 3)).append("Time Unit: ").append(timeUnit).append('\n');
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}

