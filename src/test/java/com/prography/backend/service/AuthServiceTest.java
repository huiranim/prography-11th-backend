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

/**
 * AuthService 단위 테스트
 *
 * - Spring Context, H2 DB 없이 순수 로직만 검증
 * - @ExtendWith(MockitoExtension.class): JUnit5에서 Mockito를 활성화
 * - @Mock: 실제 구현 대신 가짜 객체(Mock)를 주입. 메서드 호출 시 기본적으로 null/0/empty 반환
 * - @InjectMocks: @Mock으로 만든 가짜 객체들을 AuthService 생성자에 자동 주입
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks AuthService authService;           // 테스트 대상 (진짜 객체)
    @Mock MemberRepository memberRepository;        // DB 접근 없는 가짜 Repository
    @Mock BCryptPasswordEncoder passwordEncoder;    // BCrypt 연산 없는 가짜 인코더

    /**
     * 정상 로그인 케이스
     * - memberRepository.findByLoginId()가 회원을 반환한다고 가정
     * - passwordEncoder.matches()가 true를 반환한다고 가정
     * - 서비스가 해당 Member 객체를 그대로 반환하는지 검증
     */
    @Test
    void login_success() {
        Member member = Member.builder().loginId("admin").password("hashed")
                .status(MemberStatus.ACTIVE).role(MemberRole.ADMIN).build();
        // "admin"으로 조회하면 위 member 객체 반환하도록 Mock 행동 지정
        when(memberRepository.findByLoginId("admin")).thenReturn(Optional.of(member));
        // 비밀번호 매칭 결과를 true로 지정 (실제 BCrypt 연산 없음)
        when(passwordEncoder.matches("admin1234", "hashed")).thenReturn(true);

        Member result = authService.login(new LoginRequest("admin", "admin1234"));
        assertThat(result.getLoginId()).isEqualTo("admin");
    }

    /**
     * 존재하지 않는 loginId로 로그인 시도
     * - findByLoginId()가 Optional.empty()를 반환 → 회원 없음
     * - 서비스가 LOGIN_FAILED 에러를 던지는지 검증
     * - 보안상 "loginId 없음"과 "비밀번호 틀림"을 동일한 에러로 처리해 계정 존재 여부를 노출하지 않음
     */
    @Test
    void login_failsWhenLoginIdNotFound() {
        when(memberRepository.findByLoginId("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "pw")))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.LOGIN_FAILED.getMessage());
    }

    /**
     * 비밀번호 불일치로 로그인 실패
     * - 회원은 존재하지만 passwordEncoder.matches()가 false 반환
     * - 서비스가 LOGIN_FAILED 에러를 던지는지 검증
     */
    @Test
    void login_failsWhenPasswordMismatch() {
        Member member = Member.builder().loginId("admin").password("hashed")
                .status(MemberStatus.ACTIVE).build();
        when(memberRepository.findByLoginId("admin")).thenReturn(Optional.of(member));
        // 비밀번호 불일치 상황을 Mock으로 시뮬레이션
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.LOGIN_FAILED.getMessage());
    }

    /**
     * 탈퇴한 회원 로그인 시도
     * - 비밀번호는 맞지만 status가 WITHDRAWN인 경우
     * - 서비스가 MEMBER_WITHDRAWN 에러를 던지는지 검증
     * - LOGIN_FAILED가 아닌 별도 에러 코드로 구분 (탈퇴 사실 안내)
     */
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
