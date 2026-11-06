package es.jonay.kb.shopsystem.api.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfiguration {

	private final JwtFilter jwtAuthFilter;
	private final List<String> allowedOrigins;

	public SecurityConfiguration(JwtFilter jwtAuthFilter,
			@Value("${cors.allowed-origins:https://kioscobotanico.com,https://www.kioscobotanico.com}") String origins) {
		this.jwtAuthFilter = jwtAuthFilter;
		this.allowedOrigins = Arrays.stream(origins.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Reenvío interno a /error: sin esto, un 400/500 de una petición autenticada
						// llegaría al cliente como 401 (y el frontend cerraría la sesión).
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers("/error").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
						// Imágenes de productos y categorías: públicas (solo lectura) para que el navegador las cachee.
						.requestMatchers(HttpMethod.GET, "/images/**").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setContentType("application/json");
					response.getWriter().write("{\"error\": \"No autenticado o token caducado\"}");
				}))
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
				.headers(headers -> headers.frameOptions(f -> f.deny()));

		return http.build();
	}

	@Bean
	public UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setExposedHeaders(List.of("Content-Disposition"));
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
