package es.jonay.kb.shopsystem.controller;

import org.springframework.stereotype.Controller;

import es.jonay.kb.shopsystem.model.entities.User;
import es.jonay.kb.shopsystem.model.repository.IUserRepository;

@Controller
public class UserController {

    private IUserRepository userRepository;

    public UserController(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findByEmail(String mail) {
        return userRepository.findByEmail(mail);
    }

}
