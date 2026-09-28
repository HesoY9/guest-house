package com.hesoy9.guesthouse.repository;

import com.hesoy9.guesthouse.entity.Role;
import com.hesoy9.guesthouse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // used at login - look the user up by username, then check their password hash
    Optional<User> findByUsername(String username);

    List<User> findByRole(Role role);
}
