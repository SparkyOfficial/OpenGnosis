package com.opengnosis.iam.security

import com.opengnosis.common.security.JwtTokenProvider
import com.opengnosis.iam.service.JwtTokenValidator
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenValidator: JwtTokenValidator
) : OncePerRequestFilter() {
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request)
            
            if (token != null && jwtTokenValidator.validateToken(token)) {
                val userId = jwtTokenProvider.getUserIdFromToken(token)
                val claims = jwtTokenProvider.parseToken(token)
                val roles = claims["roles"]?.toString()?.split(",") ?: emptyList()
                
                val authorities = roles.map { SimpleGrantedAuthority(it) }
                val authentication = UsernamePasswordAuthenticationToken(userId, null, authorities)
                
                SecurityContextHolder.getContext().authentication = authentication
                
                // Add userId to request attributes for easy access
                request.setAttribute("userId", userId)
            }
        } catch (ex: Exception) {
            logger.error("Could not set user authentication in security context", ex)
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
