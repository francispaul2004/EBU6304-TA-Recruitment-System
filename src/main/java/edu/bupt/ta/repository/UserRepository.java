package edu.bupt.ta.repository;

import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.model.User;

import java.util.Optional;

public class UserRepository extends AbstractJsonRepository<User, String> {

    public UserRepository() {
        super(AppPaths.usersJson(), User.class);
    }

    public Optional<User> findByUsername(String username) {
        return findAll().stream()
                .filter(user -> user.getUsername() != null && user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }
}
