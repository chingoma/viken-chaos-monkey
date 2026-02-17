package viken.chaos.monkey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import viken.chaos.monkey.modules.chaos.adapters.ChaosK8sProperties;
import viken.chaos.monkey.modules.chaos.safety.ChaosSafetyProperties;
import viken.chaos.monkey.modules.chaos.scheduler.ChaosSchedulerProperties;
import viken.chaos.monkey.security.ChaosSecurityProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        ChaosSafetyProperties.class,
        ChaosSchedulerProperties.class,
        ChaosK8sProperties.class,
        ChaosSecurityProperties.class
})
public class VikenChaosMonkeyApplication {

    public static void main(String[] args) {
        SpringApplication.run(VikenChaosMonkeyApplication.class, args);
    }
}
