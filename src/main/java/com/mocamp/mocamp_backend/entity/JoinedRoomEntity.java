package com.mocamp.mocamp_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
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
    @Column(name = "is_participating", nullable = false)
    private Boolean isParticipating = false;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Builder.Default
    @Column(name = "resolution", nullable = false)
    private String resolution = "";


    @OneToMany(mappedBy = "joinedRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoalEntity> goals = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    public void updateResolution(String resolution) {this.resolution = resolution;}
    public void updateIsAdmin(Boolean isAdmin) {this.isAdmin = isAdmin;}
}
