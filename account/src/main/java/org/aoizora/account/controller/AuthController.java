package org.aoizora.account.controller;

import lombok.RequiredArgsConstructor;
import org.aoizora.account.controller.converter.UserConverter;
import org.aoizora.account.dto.AuthRequest;
import org.aoizora.account.dto.AuthResponse;
import org.aoizora.account.dto.RegisterRequest;
import org.aoizora.account.dto.UserDTO;
import org.aoizora.account.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserConverter userConverter;

    @PostMapping("/register")
    public UserDTO register(@RequestBody RegisterRequest request) {
        return userConverter.convert(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }
}
