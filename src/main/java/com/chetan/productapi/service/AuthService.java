package com.chetan.productapi.service;

import com.chetan.productapi.dto.auth.AuthRequest;
import com.chetan.productapi.dto.auth.AuthResponse;
import com.chetan.productapi.dto.auth.RegisterRequest;
import com.chetan.productapi.entity.Role;
import com.chetan.productapi.entity.User;
import com.chetan.productapi.repository.UserRepository;
import com.chetan.productapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        User user = refreshTokenService.validateAndConsume(refreshToken);
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.issueRefreshToken(user).getToken();
        return new AuthResponse(accessToken, newRefreshToken, "Bearer", jwtService.getAccessTokenExpirySeconds());
    }
}
