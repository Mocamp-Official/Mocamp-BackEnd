package com.mocamp.mocamp_backend.entity;

import com.mocamp.mocamp_backend.service.image.ImageType;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "image")
public class ImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageType type;

    @Column(nullable = false)
    private String path;


    @OneToOne(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserEntity user;

    @OneToOne(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true)
    private RoomEntity room;
}
