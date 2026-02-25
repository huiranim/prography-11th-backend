package com.prography.backend.service;

import com.prography.backend.domain.Member;
import com.prography.backend.domain.MemberRole;
import com.prography.backend.domain.MemberStatus;
import com.prography.backend.dto.request.LoginRequest;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks AuthService authService;
    @Mock MemberRepository memberRepository;
    @Mock BCryptPasswordEncoder passwordEncoder;

    @Test
    void login_success() {
        Member member = Member.builder().loginId("admin").password("hashed")
                .status(MemberStatus.ACTIVE).role(MemberRole.ADMIN).build();
        when(memberRepository.findByLoginId("admin")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("admin1234", "hashed")).thenReturn(true);

        Member result = authService.login(new LoginRequest("admin", "admin1234"));
        assertThat(result.getLoginId()).isEqualTo("admin");
    }

    @Test
    void login_failsWhenLoginIdNotFound() {
        when(memberRepository.findByLoginId("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "pw")))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.LOGIN_FAILED.getMessage());
    }

    @Test
    void login_failsWhenPasswordMismatch() {
        Member member = Member.builder().loginId("admin").password("hashed")
                .status(MemberStatus.ACTIVE).build();
        when(memberRepository.findByLoginId("admin")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.LOGIN_FAILED.getMessage());
    }

    @Test
    void login_failsWhenMemberWithdrawn() {
        Member member = Member.builder().loginId("user").password("hashed")
                .status(MemberStatus.WITHDRAWN).build();
        when(memberRepository.findByLoginId("user")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user", "pw")))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.MEMBER_WITHDRAWN.getMessage());
    }
}
