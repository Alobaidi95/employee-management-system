package com.example.EmployeeManagementSystem.security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;


class JwtUtilTest {

    private JwtUtil jwtUtil;

    // A valid, throwaway 256-bit key - never used for anything but this test.
    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString(Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L); // 1 hour
    }

    @Nested
    class GenerateToken {

        @Test
        void producesAWellFormedJwt() {
            String token = jwtUtil.generateToken("jdoe", "EMPLOYEE");

            assertThat(token).isNotBlank();
            // header.payload.signature
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        void differentUsersProduceDifferentTokens() {
            String tokenA = jwtUtil.generateToken("jdoe", "EMPLOYEE");
            String tokenB = jwtUtil.generateToken("msmith", "MANAGER");

            assertThat(tokenA).isNotEqualTo(tokenB);
        }
    }

    @Nested
    class ExtractUsername {

        @Test
        void returnsTheUsernameTheTokenWasIssuedFor() {
            String token = jwtUtil.generateToken("jdoe", "EMPLOYEE");

            assertThat(jwtUtil.extractUsername(token)).isEqualTo("jdoe");
        }
    }

    @Nested
    class IsTokenValid {

        @Test
        void trueForAFreshlyIssuedToken() {
            String token = jwtUtil.generateToken("jdoe", "EMPLOYEE");

            assertThat(jwtUtil.isTokenValid(token)).isTrue();
        }

        @Test
        void falseForGarbageInput() {
            assertThat(jwtUtil.isTokenValid("not-a-real-jwt")).isFalse();
        }

        @Test
        void falseForAnEmptyString() {
            assertThat(jwtUtil.isTokenValid("")).isFalse();
        }

        @Test
        void falseForAnExpiredToken() {
            // negative expiration -> the token's exp claim is already in the past
            ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
            String expiredToken = jwtUtil.generateToken("jdoe", "EMPLOYEE");

            assertThat(jwtUtil.isTokenValid(expiredToken)).isFalse();
        }

        @Test
        void falseForATokenSignedWithADifferentKey() {
            String token = jwtUtil.generateToken("jdoe", "EMPLOYEE");

            // A second JwtUtil instance with a different secret represents
            // what happens if someone forges a token, or if jwt.secret
            // ever changes between issuing and validating a token.
            JwtUtil differentKeyUtil = new JwtUtil();
            String otherSecret = Base64.getEncoder().encodeToString(
                    Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());
            ReflectionTestUtils.setField(differentKeyUtil, "secretKey", otherSecret);
            ReflectionTestUtils.setField(differentKeyUtil, "expiration", 3600000L);

            assertThat(differentKeyUtil.isTokenValid(token)).isFalse();
        }
    }
}