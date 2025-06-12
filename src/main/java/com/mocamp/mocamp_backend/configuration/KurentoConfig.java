package com.mocamp.mocamp_backend.configuration;

import com.mocamp.mocamp_backend.service.rtc.UserRegistry;
import org.kurento.client.KurentoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class KurentoConfig {

    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create("ws://kurento:8888/kurento");
    }

    @Bean
    public UserRegistry userRegistry() {
        return new UserRegistry();
    }
}
