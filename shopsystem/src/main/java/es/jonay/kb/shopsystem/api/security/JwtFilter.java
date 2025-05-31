package es.jonay.kb.shopsystem.api.security;

import java.io.IOException;

import java.util.Map;
import org.springframework.lang.NonNull;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.exceptions.JWTVerificationException;

import es.jonay.kb.shopsystem.api.utils.JwtAuthenticationHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);

        String[] allowedRoutes = { "/kiosco/swagger-ui.html",
                "/kiosco/swagger-ui/", "/kiosco/v2/",
                "configuration/", "/kiosco/swagger",
                "/kiosco/graphiql",
                "/kiosco/webjars/", "/kiosco/auth/",
                "/kiosco/api/register", "/kiosco/v3/",
                "/kiosco/graphiql", "/kiosco/graphql/schema",
                "/kiosco/websocket", "/kiosco/index.html", "/kiosco/h2-console", "/kiosco/services"
        };
        String path = request.getRequestURI();

        for (String ruta : allowedRoutes) {
            if (path.startsWith(ruta)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        if (header == null && path.startsWith("/graphql")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (header != null && header.startsWith(AUTH_PREFIX)) {

            String token = jwtAuthenticationHelper.extractTokenFromHeader(header, AUTH_PREFIX);
            try {
                Map<String, String> mapInfoToken = jwtTokenManager.validateAndGetClaims(token);
                final String correo = mapInfoToken.get("mail");
                final String rol = mapInfoToken.get("role");

                UserDetails userDetails = jwtAuthenticationHelper.buildUserDetails(correo, rol);

                jwtAuthenticationHelper.setAuthentication(userDetails, request, filterChain, response);
            } catch (JWTVerificationException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid token.\"}");

            }

        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"You are not authenticated. Login on host/api/login.\"}");
        }

    }
}
