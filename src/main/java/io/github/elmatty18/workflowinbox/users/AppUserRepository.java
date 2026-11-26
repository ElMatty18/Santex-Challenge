package io.github.elmatty18.workflowinbox.users;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Package-private on purpose: other modules go through {@link UserDirectory}.
 */
interface AppUserRepository extends JpaRepository<AppUser, String> {

}
