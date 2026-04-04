package com.workhub.saasbackend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.workhub.saasbackend.dto.request.LoginRequest;
import com.workhub.saasbackend.entity.UserRole;
import com.workhub.saasbackend.security.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

	private final JwtService jwtService;

	public AuthController(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		String email = request.getEmail();
		UUID userId = UUID.nameUUIDFromBytes(email.getBytes());
		String tenantId = resolveTenantIdFromEmail(email);
		UserRole role = email.toLowerCase().startsWith("admin") ? UserRole.ADMIN : UserRole.MEMBER;
		String token = jwtService.generateToken(userId.toString(), tenantId, role);
		return new LoginResponse(token, userId.toString(), tenantId, role.name());
	}

	@GetMapping("/me")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public MeResponse me() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return new MeResponse(null, null, null);
		}
		Object details = authentication.getDetails();
		if (details instanceof JwtService.JwtClaims claims) {
			return new MeResponse(claims.userId(), claims.tenantId(), claims.role().name());
		}
		return new MeResponse(authentication.getName(), null, null);
	}

	private String resolveTenantIdFromEmail(String email) {
		int atIdx = email.indexOf('@');
		if (atIdx > 0 && atIdx < email.length() - 1) {
			String domain = email.substring(atIdx + 1).toLowerCase();
			return switch (domain) {
				case "a.com" -> "tenant-a";
				case "b.com" -> "tenant-b";
				default -> "tenant-default";
			};
		}
		return "tenant-default";
	}

	public record LoginResponse(String token, String userId, String tenantId, String role) {
	}

	public record MeResponse(String userId, String tenantId, String role) {
	}
}

