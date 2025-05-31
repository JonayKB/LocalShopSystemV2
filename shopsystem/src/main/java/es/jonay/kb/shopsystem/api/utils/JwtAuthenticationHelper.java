package es.jonay.kb.shopsystem.api.utils;

import com.auth0.jwt.exceptions.JWTVerificationException;

import es.jonay.kb.shopsystem.api.security.JwtService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationHelper {

    /**
     * Extracts the token from the Authorization header (removing "Bearer " if
     * present).
     */
    public String extractTokenFromHeader(String header, String prefix) {
        if (header != null && header.startsWith(prefix)) {
            return header.substring(prefix.length()).trim();
        }
        return null;
    }

    /**
     * Validates the token and returns the claims as a map.
     */
    public Map<String, String> validateAndGetClaims(String token, JwtService jwtTokenManager)
            throws JWTVerificationException {
        return jwtTokenManager.validateAndGetClaims(token);
    }

    /**
     * Builds a UserDetails object from claims.
     * 
     */
    public UserDetails buildUserDetails(String correo, String rol) {
        return new UserDetails() {
            String username = correo;

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority(rol));
                return authorities;
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public boolean isAccountNonExpired() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
    }

    /**
     * Sets the authentication in the SecurityContext.
     */
    public void setAuthentication(UserDetails userDetails, HttpServletRequest request,
            FilterChain filterChain, HttpServletResponse response) throws IOException, ServletException {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        if (request != null) {
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            filterChain.doFilter(request, response);

        }
    }
}