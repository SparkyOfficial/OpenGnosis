package com.opengnosis.common.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtTokenProvider {

    @Value("\${jwt.secret:opengnosis-secret-key-change-in-production-minimum-256-bits}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration:86400000}") // 24 hours
    private var jwtExpiration: Long = 86400000

    private val key: Key by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateToken(userId: String, email: String, roles: Set<String>): String {
        return generateToken(userId, email, roles, jwtExpiration)
    }

    fun generateToken(userId: String, email: String, roles: Set<String>, expirationMillis: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMillis)

        return Jwts.builder()
            .setSubject(userId)
            .claim("email", email)
            .claim("roles", roles.joinToString(","))
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun getUserIdFromToken(token: String): String {
        val claims = parseToken(token)
        return claims.subject
    }

    fun validateToken(token: String): Boolean {
        return try {
            parseToken(token)
            true
        } catch (ex: Exception) {
            false
        }
    }

    fun parseToken(token: String): Claims {
        return Jwts.parser()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
}
