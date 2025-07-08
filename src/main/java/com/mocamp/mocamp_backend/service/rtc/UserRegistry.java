package com.mocamp.mocamp_backend.service.rtc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UserRegistry {

    private final ConcurrentHashMap<String, UserSession> usersByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserSession> usersBySessionId = new ConcurrentHashMap<>();

    public void register(UserSession user) {
        usersByName.put(user.getName(), user);
        usersBySessionId.put(user.getSession().getId(), user);
    }

    public UserSession getByName(String name) {
        return usersByName.get(name);
    }

    public UserSession getBySession(WebSocketSession session) {
        return usersBySessionId.get(session.getId());
    }

    public boolean exists(String name) {
        return usersByName.keySet().contains(name);
    }

    public UserSession removeBySession(WebSocketSession session) {
        final UserSession user = getBySession(session);

        if (user == null) {
            log.warn("Trying to remove user, but no session found for id={}", session.getId());
            usersBySessionId.remove(session.getId()); // 혹시 남아 있으면 정리
            return null;
        }

        log.info("Removing user {}", user);
        usersByName.remove(user.getName());
        log.info("Removing session {}", session);
        usersBySessionId.remove(session.getId());
        return user;
    }

}
