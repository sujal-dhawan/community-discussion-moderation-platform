package com.communityplatform.community.service;

import com.communityplatform.community.dto.request.CreateCommunityRequest;
import com.communityplatform.community.dto.response.CommunityResponse;
import com.communityplatform.community.entity.Community;
import com.communityplatform.community.entity.User;
import com.communityplatform.community.exception.DuplicateResourceException;
import com.communityplatform.community.exception.ResourceNotFoundException;
import com.communityplatform.community.repository.CommunityRepository;
import com.communityplatform.community.repository.PostRepository;
import com.communityplatform.community.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CommunityService — all business logic for the Community module.
 *
 * ── Responsibilities ──────────────────────────────────────────
 *  - Create a new community (MODERATOR only — enforced at controller level)
 *  - List all communities
 *  - Get a single community by id
 *  - Map Community entities → CommunityResponse DTOs
 *
 * ── Dependencies ─────────────────────────────────────────────
 *  CommunityRepository  — reads and writes the communities table
 *  PostRepository       — used only for countByCommunityId() to
 *                         populate postCount in the response DTO
 *  SecurityUtils        — gets the currently authenticated User entity
 *
 * ── Architecture note ────────────────────────────────────────
 *  The service NEVER returns entity objects directly. It always
 *  converts entities to response DTOs using the private toResponse()
 *  helper. This ensures:
 *    1. Internal fields (e.g. the full User entity for createdBy)
 *       are never accidentally serialised to JSON.
 *    2. The API contract stays stable even if the entity changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final PostRepository      postRepository;
    private final SecurityUtils       securityUtils;

    // ── CREATE ────────────────────────────────────────────────────
    /**
     * Creates a new community.
     *
     * Steps:
     *  1. Check the name is not already taken (application-level guard)
     *  2. Get the logged-in user as the creator
     *  3. Build and save the Community entity
     *  4. Return a CommunityResponse DTO
     *
     * The role check (MODERATOR only) is enforced at the controller level
     * via @PreAuthorize — this service method trusts that the caller
     * has already been authorised.
     *
     * @Transactional — wraps the save in a transaction so any failure
     * is rolled back cleanly.
     */
    @Transactional
    public CommunityResponse createCommunity(CreateCommunityRequest request) {
        // ── Duplicate name guard ──────────────────────────────────
        if (communityRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "A community named '" + request.getName() + "' already exists"
            );
        }

        // ── Resolve the creator ───────────────────────────────────
        User creator = securityUtils.getCurrentUser();

        // ── Build and persist ─────────────────────────────────────
        Community community = Community.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(creator)
                .build();

        Community saved = communityRepository.save(community);
        log.info("Community created: id={}, name='{}', by userId={}",
                saved.getId(), saved.getName(), creator.getId());

        return toResponse(saved);
    }

    // ── READ ALL ──────────────────────────────────────────────────
    /**
     * Returns all communities with their live post counts.
     *
     * No pagination here — in a real system with thousands of communities
     * you would add Pageable. For this project a simple List is fine and
     * easier to test and explain.
     *
     * @Transactional(readOnly = true) — tells Hibernate this is a read-only
     * transaction. Hibernate skips dirty-checking on all loaded entities
     * (it won't track changes for a flush at the end), which is a small
     * but real performance optimisation on every read call.
     */
    @Transactional(readOnly = true)
    public List<CommunityResponse> getAllCommunities() {
        return communityRepository.findAll()
                .stream()
                .map(this::toResponse)      // entity → DTO for each community
                .toList();                  // Java 16+ — equivalent to collect(toList())
    }

    // ── READ ONE ──────────────────────────────────────────────────
    /**
     * Returns a single community by its id.
     *
     * Throws ResourceNotFoundException (→ 404) if no community exists
     * with the given id. This is caught by GlobalExceptionHandler.
     */
    @Transactional(readOnly = true)
    public CommunityResponse getCommunityById(Long id) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Community not found with id: " + id
                ));
        return toResponse(community);
    }

    // ── GET BY NAME ───────────────────────────────────────────────
    /**
     * Looks up a community by its unique name.
     * Useful for URL slugs like /api/communities/name/java.
     */
    @Transactional(readOnly = true)
    public CommunityResponse getCommunityByName(String name) {
        Community community = communityRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Community not found with name: " + name
                ));
        return toResponse(community);
    }

    // ── Private helper: Entity → DTO ─────────────────────────────
    /**
     * Converts a Community entity into a CommunityResponse DTO.
     *
     * Called by every public method — the single place where the
     * mapping logic lives. If the response shape ever changes,
     * only this method needs updating.
     *
     * Note: postCount makes one extra DB query per community.
     * In a high-traffic system you'd denormalise this as a counter
     * column on the community (like upvotes on Post), or batch-load
     * counts for all communities in one query. For this project,
     * one extra COUNT query per community is perfectly acceptable.
     */
    private CommunityResponse toResponse(Community community) {
        long postCount = postRepository.countByCommunityIdAndIsDeletedFalse(
                community.getId()
        );

        return CommunityResponse.builder()
                .id(community.getId())
                .name(community.getName())
                .description(community.getDescription())
                .createdByUsername(community.getCreatedBy().getUsername())
                .postCount(postCount)
                .createdAt(community.getCreatedAt())
                .build();
    }
}
