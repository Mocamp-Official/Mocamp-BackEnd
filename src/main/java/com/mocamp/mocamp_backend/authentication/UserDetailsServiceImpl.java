package com.mocamp.mocamp_backend.authentication;

import java.util.Collection;
import java.util.Collections;

import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private static final String USER_NOT_FOUND_EXCEPTION = "존재하지 않는 회원입니다.";

    private Collection<? extends GrantedAuthority> getAuthorities(Object user) {
        // 권한 정보를 GrantedAuthority 객체 컬렉션으로 반환
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * 로그인 시, DB에 있는 사용자 정보와 일치하는지 확인하고 Security가 이해할 수 있는 UserDetails로 반환해주는 메서드
     * UserDetailsService는 자동으로 작동하되, 이를 Override하여 해당 메서드로 작동하게 구성
     * @param email
     * @return UserDetails 객체 반환
     * @throws UsernameNotFoundException -> DB에 해당 사용자 없으면 예외 처리
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_EXCEPTION));
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), getAuthorities(user));
    }

    /**
     * JWT 필터에서 인증 성공 후, SecurityContextHolder에 담긴 인증 객체 불러와 요청한 user 찾는 메서드
     * @return UserEntity
     */
    public UserEntity getUserByContextHolder() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;
        return userRepository.findUserByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_EXCEPTION));
    }

    public Authentication getAuthentication(){
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
