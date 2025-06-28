package com.mocamp.mocamp_backend.service.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserGoalData {
    private String date;
    private Long amount;
}
