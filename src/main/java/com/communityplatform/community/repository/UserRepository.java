package com.communityplatform.community.repository;

import com.communityplatform.community.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the User entity.
 *
 * ── What JpaRepository<User, Long> gives you for free ───────
 *  save(user)                  → INSERT or UPDATE
 *  findById(id)                → SELECT WHERE id = ?
 *  findAll()                   → SELECT all rows
 *  deleteById(id)              → DELETE WHERE id = ?
 *  count()                     → SELECT COUNT(*)
 *  existsById(id)              → SELECT 1 WHERE id = ?
 *  ... and more
 *
 * ── Custom methods ───────────────────────────────────────────
 *  Spring Data JPA reads the method name and automatically
 *  generates the query — no SQL or JPQL needed.
 *
 *  findByEmail(email)
 *    → SELECT * FROM users WHERE email = ?
 *    Used by: AuthService (login), UserDetailsServiceImpl (JWT filter)
 *
 *  findByUsername(username)
 *    → SELECT * FROM users WHERE username = ?
 *    Used by: AuthService (registration duplicate check)
 *
 *  existsByEmail(email)
 *    → SELECT 1 FROM users WHERE email = ? LIMIT 1
 *    Returns boolean — cheaper than loading the full User object
 *    just to check if it exists. Used during registration validation.
 *
 *  existsByUsername(username)
 *    → SELECT 1 FROM users WHERE username = ? LIMIT 1
 *    Same pattern — fast duplicate check on username at registration.
 *
 * ── Interview talking point ──────────────────────────────────
 *  Spring Data JPA uses a naming convention called "Derived Query
 *  Methods". It parses the method name — findBy, existsBy, countBy —
 *  then reads the field name after it — Email, Username — and
 *  generates the WHERE clause automatically at startup. No SQL
 *  string literals, no typos in column names.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used by Spring Security's UserDetailsService to load a user by email at login
    Optional<User> findByEmail(String email);

    // Used during registration to check username availability
    Optional<User> findByUsername(String username);

    // Fast existence check — avoids loading the full User object
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
