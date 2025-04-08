package com.mocamp.mocamp_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "joined_room")
public class JoinedRoomEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "joined_room_id", nullable = false)
    private Long joinedRoomId;

    @Builder.Default
    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean participating = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;


    @OneToMany(mappedBy = "joinedRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalEntity> goals;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;
}
