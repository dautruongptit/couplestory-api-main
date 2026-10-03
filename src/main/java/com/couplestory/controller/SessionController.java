package com.couplestory.controller;

import com.couplestory.entity.RefreshToken;
import com.couplestory.entity.User;
import com.couplestory.repository.RefreshTokenRepository;
import com.couplestory.repository.UserRepository;
import com.couplestory.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getSessions(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(refreshTokenRepository.findByUserId(userDetails.getId()));
    }

    /** Đăng xuất mọi thiết bị: thu hồi refresh token và tăng tokenVersion để vô hiệu hóa mọi JWT đang có. */
    @DeleteMapping
    @Transactional
    public ResponseEntity<?> clearSessions(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userDetails.getId());
        tokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);

        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
