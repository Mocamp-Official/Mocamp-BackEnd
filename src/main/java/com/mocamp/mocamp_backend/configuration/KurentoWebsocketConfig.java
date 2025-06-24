package com.mocamp.mocamp_backend.configuration;

import com.mocamp.mocamp_backend.service.rtc.CallHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class KurentoWebsocketConfig implements WebSocketConfigurer {

    private final CallHandler callHandler;
//    private final KurentoInterceptor kurentoInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.addHandler(callHandler, "/groupcall").addInterceptors(kurentoInterceptor).setAllowedOrigins("*");
        registry.addHandler(callHandler, "/groupcall");
    }

}
