package io.github.elmatty18.workflowinbox.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Users are seeded by Flyway and read-only for the application.
 */
@Entity
@Table(name = "app_user")
class AppUser {

	@Id
	private String id;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	protected AppUser() {
		// for JPA
	}

	User toUser() {
		return new User(id, displayName, role);
	}

}
