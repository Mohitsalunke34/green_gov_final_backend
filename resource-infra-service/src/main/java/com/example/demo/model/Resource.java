package com.example.demo.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import enums.ResourceStatus;
import enums.ResourceType;
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
@Table(name = "resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resourceId;

    private Long projectId;
    
    @Column(name = "project_title")
    private String projectName;

    @Column(nullable = false)
    private String resourceName;

    @Enumerated(EnumType.STRING)
    private ResourceType type;

    @Column(nullable = false)
    private Double totalQuantity;

    @Column(nullable = false)
    private Double availableQuantity;

    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;
    

    
}
