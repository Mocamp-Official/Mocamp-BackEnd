package com.mocamp.mocamp_backend.dto.notice;

import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeUpdateResponse {

    private WebsocketMessageType type;
    private String content;
}
