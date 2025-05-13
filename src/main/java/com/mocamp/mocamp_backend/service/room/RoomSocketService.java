package com.mocamp.mocamp_backend.service.room;

import com.mocamp.mocamp_backend.repository.RoomRepository;
import com.mocamp.mocamp_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomSocketService {
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
}
