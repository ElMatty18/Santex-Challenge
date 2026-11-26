package io.github.elmatty18.workflowinbox.users;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class JpaUserDirectory implements UserDirectory {

	private final AppUserRepository repository;

	JpaUserDirectory(AppUserRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<User> findById(String id) {
		return repository.findById(id).map(AppUser::toUser);
	}

}
