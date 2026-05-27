package com.example.demo.dto;

import lombok.Data;

@Data
public class UserBasicDTO {
	private Long id;
	private String username;
	private String email;
	private String primaryRole;
	private boolean active;
}