package es.jonay.kb.shopsystem.api.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.exceptions.JWTVerificationException;

import es.jonay.kb.shopsystem.api.utils.JwtAuthenticationHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Si la petición trae un token de acceso válido, autentica al usuario. Si no trae
 * token o no es válido, sigue sin autenticar: qué rutas son públicas lo decide
 * únicamente {@link SecurityConfiguration}, que responde 401 en las protegidas.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_PREFIX = "Bearer ";

    private final JwtService jwtTokenManager;
    private final JwtAuthenticationHelper jwtAuthenticationHelper;

    public JwtFilter(JwtService jwtTokenManager, JwtAuthenticationHelper jwtAuthenticationHelper) {
        this.jwtTokenManager = jwtTokenManager;
        this.jwtAuthenticationHelper = jwtAuthenticationHelper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(AUTH_PREFIX)) {
            String token = jwtAuthenticationHelper.extractTokenFromHeader(header, AUTH_PREFIX);
            try {
                Map<String, String> claims = jwtTokenManager.validateAndGetClaims(token);
                UserDetails userDetails = jwtAuthenticationHelper.buildUserDetails(claims.get("mail"),
                        claims.get("role"));
                jwtAuthenticationHelper.authenticate(userDetails, request);
            } catch (JWTVerificationException e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
