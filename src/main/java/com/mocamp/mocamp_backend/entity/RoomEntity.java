package com.mocamp.mocamp_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "room")
public class RoomEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "room_seq", nullable = false)
    private String roomSeq;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    @Builder.Default
    private Integer roomNum = 0;

    @Column(nullable = false)
    private Boolean status;    // true: 활동중, false: 종료

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "duration", nullable = false)
    private LocalTime duration;

    @Builder.Default
    @Column(nullable = false)
    private String notice = "공지사항을 입력해주세요";

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Builder.Default
    @Column(name = "mic_availability", nullable = false)
    private Boolean micAvailability = true;


    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JoinedRoomEntity> joinedRooms;

    @OneToOne
    @JoinColumn(name = "image_id", nullable = true)
    private ImageEntity image;

    public void updateNotice(String notice) {
        this.notice = notice;
    }


}
