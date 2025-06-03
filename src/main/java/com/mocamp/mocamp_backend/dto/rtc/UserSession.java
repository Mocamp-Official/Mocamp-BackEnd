package com.mocamp.mocamp_backend.dto.rtc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;

@Getter
@RequiredArgsConstructor
public class UserSession {

    private final MediaPipeline pipeline;
    private final WebRtcEndpoint webRtcEndpoint;

    public void release() {
        if (webRtcEndpoint != null) {
            webRtcEndpoint.release();
        }
        if (pipeline != null) {
            pipeline.release();
        }
    }
}