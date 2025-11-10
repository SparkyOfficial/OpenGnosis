package com.opengnosis.iam.controller

import com.opengnosis.iam.dto.AssignRolesRequest
import com.opengnosis.iam.dto.RoleResponse
import com.opengnosis.iam.service.RoleService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val roleService: RoleService
) {
    
    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR', 'SYSTEM_ADMIN') or #userId == authentication.principal")
    fun getUserRoles(@PathVariable userId: UUID): ResponseEntity<RoleResponse> {
        val roles = roleService.getUserRoles(userId)
        
        return ResponseEntity.ok(
            RoleResponse(
                userId = userId.toString(),
                roles = roles
            )
        )
    }
    
    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR', 'SYSTEM_ADMIN')")
    fun assignRoles(
        @PathVariable userId: UUID,
        @Valid @RequestBody request: AssignRolesRequest,
        @RequestAttribute("userId") changedBy: UUID
    ): ResponseEntity<RoleResponse> {
        val user = roleService.assignRoles(userId, request.roles, changedBy)
        
        return ResponseEntity.ok(
            RoleResponse(
                userId = user.id.toString(),
                roles = user.getRoleNames()
            )
        )
    }
}
