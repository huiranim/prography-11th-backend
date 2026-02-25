package com.prography.backend.service;

import com.prography.backend.domain.Member;
import com.prography.backend.domain.MemberStatus;
import com.prography.backend.dto.request.LoginRequest;
import com.prography.backend.exception.AppException;
import com.prography.backend.exception.ErrorCode;
import com.prography.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Member login(LoginRequest request) {
        Member member = memberRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new AppException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new AppException(ErrorCode.LOGIN_FAILED);
        }

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new AppException(ErrorCode.MEMBER_WITHDRAWN);
        }

        return member;
    }
}
