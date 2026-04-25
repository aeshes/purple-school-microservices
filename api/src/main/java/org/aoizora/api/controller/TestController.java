package org.aoizora.api.controller;

import lombok.RequiredArgsConstructor;
import org.aoizora.api.rmq.AuthClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class TestController {

    private final AuthClient authClient;

    @PostMapping("/async/login")
    public CompletableFuture<ResponseEntity<?>> asyncLogin(
            @RequestParam String email,
            @RequestParam String password) {

        return authClient.login(email, password)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        return ResponseEntity.ok(response.getData());
                    } else {
                        return ResponseEntity.badRequest().body(response.getErrorMessage());
                    }
                });
    }
}
