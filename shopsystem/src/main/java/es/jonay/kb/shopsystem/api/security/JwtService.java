package es.jonay.kb.shopsystem.api.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret = "secret";

    @Value("${jwt.expiration}")
    private long expiration = 9876543210L;

    public String generateToken(String mail, String rol) {
        return JWT.create()
                .withSubject(mail)
                .withClaim("role", rol)
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration))
                .sign(Algorithm.HMAC256(secret));
    }

    public Map<String, String> validateAndGetClaims(String token) {
        Map<String, Claim> claims = JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token)
                .getClaims();

        Map<String, String> infoToken = new HashMap<>();
        infoToken.put("mail", claims.get("sub").asString());
        infoToken.put("role", claims.get("role").asString());

        return infoToken;
    }
}