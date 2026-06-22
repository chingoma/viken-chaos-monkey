package viken.chaos.monkey.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Resolves actor identity from JWT or Basic auth principal.
 */
public final class ChaosActorResolver {

    private ChaosActorResolver() {
    }

    public static String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                return sub;
            }
            Object customerNo = jwt.getClaim("customerNo");
            if (customerNo != null) {
                return customerNo.toString();
            }
        }
        return auth.getName() != null ? auth.getName() : "api";
    }
}
