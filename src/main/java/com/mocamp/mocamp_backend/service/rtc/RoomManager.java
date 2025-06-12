package com.mocamp.mocamp_backend.service.rtc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.KurentoClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class RoomManager {

    private final KurentoClient kurento;
    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    public Room getRoom(String roomName) {
        log.debug("방 탐색 {}", roomName);
        Room room = rooms.get(roomName);

        if (room == null) {
            log.debug("방 {}은 존재하지 않습니다. 지금 방을 생성합니다.", roomName);
            room = new Room(roomName, kurento.createMediaPipeline());
            rooms.put(roomName, room);
        }
        log.debug("방 {} 존재합니다.", roomName);
        return room;
    }

    public void removeRoom(Room room) {
        this.rooms.remove(room.getName());
        room.close();
        log.info("Room {} removed and closed", room.getName());
    }

}
