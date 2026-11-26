package io.github.elmatty18.workflowinbox.users;

import java.util.Optional;

/**
 * Public entry point of the users module for looking up users by id.
 */
public interface UserDirectory {

	Optional<User> findById(String id);

}
