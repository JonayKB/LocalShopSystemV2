package es.jonay.kb.shopsystem.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.jonay.kb.shopsystem.api.security.JwtService;
import es.jonay.kb.shopsystem.model.entities.User;

@Service
public class AuthController {

    private final JwtService jwtService;
    private final UserController userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService, UserController userService, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public String login(String mail, String password) {
        User user = userService.findByEmail(mail);

        if (user != null && passwordEncoder.matches(password, user.getPassword())) {

            return jwtService.generateToken(user.getEmail(), "ROLE_ADMIN");

        }
        throw new RuntimeException("User not found or Invalid Credentials");
    }

}
