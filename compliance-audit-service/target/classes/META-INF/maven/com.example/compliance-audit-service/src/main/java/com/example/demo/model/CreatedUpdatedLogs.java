package com.example.demo.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass // the columns you declare here (e.g., created_at, updated_at, created_by,
					// updated_by) will be copied into the tables of the concrete entities that
					// extend it. You cannot query a @MappedSuperclass directly. it does not create
					// a table on its own also

@Getter
@Setter
@NoArgsConstructor // @Data -> can be used but may accidentally trigger lazyload.
public abstract class CreatedUpdatedLogs {
	@NotNull // to implement it hibernate validator should also be there in the dependency
	@Column(name = "created_at", updatable = false, nullable = false)
	private Instant createdAt;
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
	@Column(name = "created_by", length = 50, nullable = false)
	private String createdBy;
	@Column(name = "updated_by", length = 60, nullable = false)
	private String updatedBy;

	@PrePersist // Automatically set values BEFORE INSER
	public void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate // Automatically update BEFORE UPDATE
	public void onUpdate() {
		this.updatedAt = Instant.now();
	}

}
