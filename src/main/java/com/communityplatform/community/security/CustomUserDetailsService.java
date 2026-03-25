package com.communityplatform.community.security;

import com.communityplatform.community.entity.User;
import com.communityplatform.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CustomUserDetailsService — the bridge between Spring Security and our database.
 *
 * Spring Security does not know about our User entity or UserRepository.
 * It only knows about the UserDetailsService interface with one method:
 *   UserDetails loadUserByUsername(String username)
 *
 * We implement this interface so Spring Security can ask: "Give me the
 * user record for this identifier" — and we answer by querying our DB.
 *
 * ── Why "username" means "email" here ────────────────────────
 *  Spring Security's interface uses the parameter name "username"
 *  historically. In our app, login is by EMAIL — so we treat
 *  "username" as "email" throughout. SecurityConfig wires this up
 *  via the AuthenticationManager configuration.
 *
 * ── What UserDetails is ───────────────────────────────────────
 *  UserDetails is a Spring Security interface representing an
 *  authenticated principal. It carries:
 *    - getUsername()      → the identity string (email in our case)
 *    - getPassword()      → the BCrypt hash (compared by AuthenticationManager)
 *    - getAuthorities()   → the list of roles / permissions
 *    - isEnabled()        → whether the account is active
 *    - + three "account expired / locked / credentials expired" flags
 *
 *  We return Spring's built-in User.builder() implementation rather
 *  than making our own entity implement UserDetails — keeping the
 *  security concern separate from the JPA entity concern.
 *
 * ── Role prefix convention ────────────────────────────────────
 *  Spring Security requires roles to be prefixed with "ROLE_".
 *  We store "USER" and "MODERATOR" in the DB (without prefix)
 *  and add "ROLE_" here when building the authority.
 *
 *  This means in SecurityConfig and @PreAuthorize annotations:
 *    hasRole("MODERATOR")        checks for authority "ROLE_MODERATOR"
 *    hasAuthority("ROLE_USER")   also works — same thing
 *
 * ── isActive mapping ─────────────────────────────────────────
 *  Our User entity has isActive. We pass it as the `enabled` flag
 *  to Spring Security's UserDetails builder. If isActive = false,
 *  Spring Security will reject the login with DisabledException
 *  before even checking the password — a clean soft-ban mechanism.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Called by Spring Security's AuthenticationManager during login,
     * and by JwtAuthenticationFilter on every authenticated request.
     *
     * @param email the email from the login request (Spring calls it "username")
     * @return a UserDetails object Spring Security can work with
     * @throws UsernameNotFoundException if no user exists with this email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Fetch our entity from the database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email
                ));

        // Convert our entity into Spring Security's UserDetails format.
        // We use Spring's built-in org.springframework.security.core.userdetails.User
        // (note: different class from our own entity com.communityplatform...entity.User)
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())                          // identity = email
                .password(user.getPasswordHash())                   // BCrypt hash
                .authorities(buildAuthority(user))                  // ROLE_USER / ROLE_MODERATOR
                .disabled(!user.getIsActive())                      // false = account is disabled
                .accountExpired(false)                              // not implemented
                .accountLocked(false)                               // not implemented
                .credentialsExpired(false)                          // not implemented
                .build();
    }

    /**
     * Wraps the user's role string in a SimpleGrantedAuthority with the
     * required "ROLE_" prefix.
     *
     * user.getRole() returns the Role enum (USER or MODERATOR).
     * .name() converts it to the string "USER" or "MODERATOR".
     * "ROLE_" + name gives "ROLE_USER" or "ROLE_MODERATOR".
     */
    private List<SimpleGrantedAuthority> buildAuthority(User user) {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }
}
