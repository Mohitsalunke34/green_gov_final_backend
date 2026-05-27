package com.example.demo.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import enums.InfrastructureStatus;
import enums.InfrastructureType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "infrastructure")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Infrastructure {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long infraId;

	private Long projectId;

	@Column(nullable = false)
	private String infrastructureName;
	
	@Column(name = "project_title") 
	private String projectName;

	@Enumerated(EnumType.STRING)
	private InfrastructureType type;

	@Column(nullable = false)
	private String location;

	@Column(nullable = false)
	private Double capacity;

	private Double utilizedCapacity;

	@Enumerated(EnumType.STRING)
	private InfrastructureStatus status;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;
	
}
