package com.nimbusdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LoginResponse {
	private final String token;
	private final boolean requires2FA;
	private final String message;
}

