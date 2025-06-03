package com.mocamp.mocamp_backend.configuration;

import org.kurento.client.KurentoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class KurentoConfig {
    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create();
    }
}
