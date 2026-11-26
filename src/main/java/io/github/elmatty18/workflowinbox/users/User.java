package io.github.elmatty18.workflowinbox.users;

/**
 * A user as seen by other modules. The JPA entity ({@link AppUser}) never leaves this package.
 */
public record User(String id, String displayName, Role role) {

	public boolean hasRole(Role candidate) {
		return role == candidate;
	}

}
