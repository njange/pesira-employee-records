package io.pesira.records.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // Emails are stored lowercase; callers must lowercase the input first.
    Optional<AppUser> findByEmail(String email);
}
