package com.mocamp.mocamp_backend.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class RoomParticipantResponse {
    Long userId;
    String userSeq;
    String username;
    List<String> goalList;
}
