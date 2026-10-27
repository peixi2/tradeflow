package com.tradeflow.service;

import com.tradeflow.model.UserAccount;
import com.tradeflow.repository.UserAccountRepository;
import com.tradeflow.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public Map<String, Object> login(String username, String password) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("账号或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return Map.of(
                "token", jwtUtils.generate(user.getId(), user.getUsername(), user.getRole()),
                "user", Map.of("id", user.getId(), "username", user.getUsername(), "displayName", user.getDisplayName(), "role", user.getRole())
        );
    }
}

