package com.mocamp.mocamp_backend.service.rtc;

import com.mocamp.mocamp_backend.authentication.UserDetailsServiceImpl;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.rtc.IceCandidateDto;
import com.mocamp.mocamp_backend.dto.rtc.SdpAnswerResponse;
import com.mocamp.mocamp_backend.dto.rtc.SdpOfferRequest;
import com.mocamp.mocamp_backend.dto.rtc.UserSession;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketErrorMessage;
import com.mocamp.mocamp_backend.entity.JoinedRoomEntity;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WebRtcService {

    private static final String ROOM_NOT_FOUND_MESSAGE = "방을 찾을 수 없습니다";
    private static final String USER_NOT_IN_ROOM_MESSAGE = "해당 방에 참여 중인 유저가 아닙니다";

    private final KurentoClient kurentoClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository roomRepository;
    private final JoinedRoomRepository joinedRoomRepository;
    private final UserDetailsServiceImpl userDetailsService;

    // roomId -> pipeline
    // 방마다 하나의 미디어 파이프라인을 유지하려고 만든 Map
    private final Map<Long, MediaPipeline> pipelines = new ConcurrentHashMap<>();
    // roomId -> (userId -> UserSession)
    // 방마다 그 안에 있는 사용자들의 세션 정보를 가지고 있음
    private final Map<Long, Map<Long, UserSession>> roomUserSessions = new ConcurrentHashMap<>();


    /**
     * WebRTC 연결을 위한 SDP Offer를 처리하는 메서드
     * 이 메서드는 클라이언트(브라우저 등)가 WebRTC 연결을 시작하기 위해 보내는 SDP Offer를 받아,
     * Kurento Media Server에서 WebRtcEndpoint를 생성하고 SDP Answer를 만들어 다시 클라이언트에게 전달한다.
     *
     * 1. 현재 로그인한 유저 정보를 가져온다.
     * 2. 요청받은 roomId에 해당하는 방이 존재하는지 확인한다.
     * 3. 사용자가 해당 방에 참여 중인지 확인한다.
     * 4. 해당 방의 MediaPipeline이 없다면 새로 생성하고, 있으면 재사용한다.
     * 5. 해당 사용자에 대한 WebRtcEndpoint를 MediaPipeline 위에 생성한다.
     * 6. 받은 SDP Offer를 바탕으로 SDP Answer를 생성한다.
     * 7. ICE 후보 수집을 시작하고, ICE 후보가 수집될 때마다 WebSocket으로 클라이언트에게 전송한다.
     * 8. 생성한 WebRtcEndpoint와 pipeline을 기반으로 UserSession을 저장한다.
     * 9. SDP Answer를 클라이언트에 WebSocket으로 전송하여, 클라이언트가 remote description 설정을 할 수 있도록 한다.
     * @param sdpOfferRequest SDP Offer
     * @param roomId 현재 방 ID
     */
    public void processOffer(SdpOfferRequest sdpOfferRequest, Long roomId) {
        UserEntity user = userDetailsService.getUserByContextHolder();

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            messagingTemplate.convertAndSend("/sub/rtc/offer/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_FOUND_MESSAGE)));
            return;
        }

        // 해당하는 방에 소속하는 유저인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            messagingTemplate.convertAndSend("/sub/rtc/offer/" + roomId, new WebsocketErrorMessage(user.getUserId(), USER_NOT_IN_ROOM_MESSAGE));
            return;
        }

        // 방마다 MediaPipeline 생성 or 재사용
        MediaPipeline pipeline = pipelines.computeIfAbsent(roomId, id -> kurentoClient.createMediaPipeline());

        // 사용자 WebRtcEndpoint 생성
        WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();

        // SDP Offer 처리
        String sdpAnswer = webRtcEndpoint.processOffer(sdpOfferRequest.getSdpOffer());

        // Kurento Media Server(KMS)가 ICE 후보를 수집하기 시작하도록 트리거하는 함수
        webRtcEndpoint.gatherCandidates();
        // ICE 후보를 찾으면 이벤트를 발생시킴
        webRtcEndpoint.addOnIceCandidateListener(event -> {
            IceCandidate candidate = event.getCandidate();
            messagingTemplate.convertAndSend("/sub/rtc/ice/" + roomId,
                    new IceCandidateDto(candidate.getCandidate(), candidate.getSdpMid(), candidate.getSdpMLineIndex(), user.getUserId()));
        });

        // 각 방마다 유저들의 세션을 없으면 새로 만들어서 저장
        roomUserSessions
                .computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(user.getUserId(), new UserSession(pipeline, webRtcEndpoint));

        // SDP Answer 클라이언트로 전송
        messagingTemplate.convertAndSend("/sub/rtc/offer/" + roomId,
                new SdpAnswerResponse(user.getUserId(), sdpAnswer));
    }

    /**
     * 클라이언트에서 보낸 ICE 후보를 받아 Kurento의 WebRtcEndpoint에 추가하는 메서드
     * @param iceCandidateDto 프론트에서 전송한 ICE 후보 정보
     * @param roomId 현재 방 ID
     */
    public void receiveIceCandidate(IceCandidateDto iceCandidateDto, Long roomId) {
        UserEntity user = userDetailsService.getUserByContextHolder();

        // 해당 roomId에서 현재 유저의 세션을 가져옴 (없으면 빈 Map)
        UserSession userSession = roomUserSessions
                .getOrDefault(roomId, Map.of())
                .get(user.getUserId());

        // 세션이 없다면 ICE 후보를 추가할 수 없으므로 종료
        if (userSession == null) {
            return;
        }

        // 프론트에서 받은 ICE 후보 정보를 Kurento에서 사용하는 객체로 변환
        IceCandidate candidate = new IceCandidate(iceCandidateDto.getCandidate(), iceCandidateDto.getSdpMid(), iceCandidateDto.getSdpMLineIndex());

        // 해당 유저의 WebRtcEndpoint에 ICE 후보를 추가
        userSession.getWebRtcEndpoint().addIceCandidate(candidate);
    }
}
