package com.example.hungdt2.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
public class WebSocketConfig implements WebSocketConfigurer {
    private final RoomWsHandler roomWsHandler;
    private final UserWsHandler userWsHandler;
    private final TokenHandshakeInterceptor tokenHandshakeInterceptor;

    public WebSocketConfig(RoomWsHandler roomWsHandler, UserWsHandler userWsHandler, TokenHandshakeInterceptor tokenHandshakeInterceptor) {
        this.roomWsHandler = roomWsHandler;
        this.userWsHandler = userWsHandler;
        this.tokenHandshakeInterceptor = tokenHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(roomWsHandler, "/ws/rooms/{roomId}")
            .addInterceptors(tokenHandshakeInterceptor)
            .setAllowedOrigins("*"); // dev: allow all origins

        registry
            .addHandler(userWsHandler, "/ws/users/{userId}")
            .addInterceptors(tokenHandshakeInterceptor)
            .setAllowedOrigins("*"); // dev: allow all origins
    }
}
