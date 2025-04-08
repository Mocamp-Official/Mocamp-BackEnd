package com.mocamp.mocamp_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private String status;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private String notice;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;


    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JoinedRoomEntity> joinedRooms;

    @OneToOne
    @JoinColumn(name = "image_id", nullable = true)
    private ImageEntity image;
}
