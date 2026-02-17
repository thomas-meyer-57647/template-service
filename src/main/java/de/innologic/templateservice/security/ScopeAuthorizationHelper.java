package de.innologic.templateservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ScopeAuthorizationHelper {

    public boolean hasScope(String scope) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String expectedAuthority = "SCOPE_" + scope;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (expectedAuthority.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}

