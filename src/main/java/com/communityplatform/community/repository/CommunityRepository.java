package com.communityplatform.community.repository;

import com.communityplatform.community.entity.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the Community entity.
 *
 * ── Custom methods ───────────────────────────────────────────
 *  findByName(name)
 *    → SELECT * FROM communities WHERE name = ?
 *    Used by: CommunityService to look up a community by its
 *    unique name handle (similar to a subreddit name).
 *
 *  existsByName(name)
 *    → SELECT 1 FROM communities WHERE name = ? LIMIT 1
 *    Used by: CommunityService during community creation to
 *    reject duplicate names before attempting an INSERT.
 *
 * ── Interview talking point ──────────────────────────────────
 *  The `name` column has a UNIQUE constraint at the database level
 *  (defined in the Community entity with unique = true on @Column).
 *  existsByName() is an application-level guard that gives us a
 *  clean error message BEFORE hitting that DB constraint. Without it,
 *  a duplicate would still be rejected — but as an ugly
 *  DataIntegrityViolationException instead of a controlled response.
 *
 *  This two-layer validation (application check + DB constraint)
 *  is a best practice: the DB constraint is the safety net,
 *  the application check is the user-friendly gate.
 */
@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

    // Look up a community by its unique name handle
    Optional<Community> findByName(String name);

    // Check before insert to give a clean "name already taken" error
    boolean existsByName(String name);
}
