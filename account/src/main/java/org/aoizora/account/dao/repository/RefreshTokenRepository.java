package org.aoizora.account.dao.repository;

import org.aoizora.account.dao.domain.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserEmail(String userEmail);
    void deleteByToken(String token);
}
