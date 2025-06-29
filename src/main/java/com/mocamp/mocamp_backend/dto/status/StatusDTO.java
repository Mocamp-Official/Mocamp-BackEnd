package com.mocamp.mocamp_backend.dto.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusDTO {

    private WebsocketMessageType type;
    private Long userId;
    private Boolean workStatus;
    private Boolean camStatus;
    private Boolean micStatus;

}
