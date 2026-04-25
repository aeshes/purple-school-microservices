package org.aoizora.account.service;

import lombok.RequiredArgsConstructor;
import org.aoizora.account.dao.domain.RefreshToken;
import org.aoizora.account.dao.domain.Role;
import org.aoizora.account.dao.domain.User;
import org.aoizora.account.dao.repository.UserRepository;
import org.aoizora.account.dto.AuthRequest;
import org.aoizora.account.dto.AuthResponse;
import org.aoizora.account.dto.RegisterRequest;
import org.aoizora.account.service.exception.EmailAlreadyExistsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public User register(RegisterRequest request) throws EmailAlreadyExistsException {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Адрес электронной почты занят.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .build();

        return userRepository.save(user);
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        String jwtToken = jwtService.generateToken(user);
        RefreshToken jwtRefreshToken = jwtService.createRefreshToken(request.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(jwtRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
