package com.mocamp.mocamp_backend.service.user;

import lombok.*;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserTimeData {
    private String date;
    private Long duration;
}
