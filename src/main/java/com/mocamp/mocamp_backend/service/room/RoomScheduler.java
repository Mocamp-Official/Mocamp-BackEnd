package com.mocamp.mocamp_backend.service.room;

import com.mocamp.mocamp_backend.dto.alert.AlertResponse;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class RoomScheduler {

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 멀티 쓰레드 환경을 고려하여 안전한 ConcurrentHaspMap 사용
    // 30분전, 20분전, 10분전 룸 ID를 기반으로 1번만 보내는 것을 보장하기 위해 key 값으로 보관
    private static final Set<Long> alreadySent30 = ConcurrentHashMap.newKeySet();
    private static final Set<Long> alreadySent20 = ConcurrentHashMap.newKeySet();
    private static final Set<Long> alreadySent10 = ConcurrentHashMap.newKeySet();

    /**
     * 모캠프 방 종료까지 30분전, 20분전, 10분전 마다 종료 알림 보내는 스케쥴러 메서드
     */
    @Scheduled(cron = "0 * * * * *") // 매 분마다
    public void checkRoomEndWarnings() {
        log.info("[종료 알림 체크 스케쥴러 작동]");
        List<RoomEntity> activeRooms = roomRepository.findAllByStatusTrue();
        LocalDateTime now = LocalDateTime.now();

        for (RoomEntity room : activeRooms) {
            LocalDateTime endTime = room.getStartedAt().plusSeconds(room.getDuration().toSecondOfDay());
            long minutesLeft = ChronoUnit.MINUTES.between(now, endTime);

            if (minutesLeft == 30 && !alreadySent30.contains(room.getRoomId())) {
                sendAlert(room, 30);
                log.info("[30분전 종료 알림] - roomId: {}", room.getRoomId());
                alreadySent30.add(room.getRoomId());
            } else if (minutesLeft == 20 && !alreadySent20.contains(room.getRoomId())) {
                sendAlert(room, 20);
                log.info("[20분전 종료 알림] - roomId: {}", room.getRoomId());
                alreadySent20.add(room.getRoomId());
            } else if (minutesLeft == 10 && !alreadySent10.contains(room.getRoomId())) {
                sendAlert(room, 10);
                log.info("[10분전 종료 알림] - roomId: {}", room.getRoomId());
                alreadySent10.add(room.getRoomId());
            }
        }
    }

    /**
     * 30분전, 20분전, 10분전 마다 웹소캣으로 알림 보내는 메서드
     * @param room Room 엔티티
     * @param minutesLeft 30분전 or 20분전 or 10분전
     */
    private void sendAlert(RoomEntity room, int minutesLeft) {
        messagingTemplate.convertAndSend("/sub/data/" + room.getRoomId(), new AlertResponse(WebsocketMessageType.ROOM_END_ALERT, minutesLeft));
    }
}
