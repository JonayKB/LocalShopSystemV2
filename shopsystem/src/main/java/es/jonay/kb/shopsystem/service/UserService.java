package es.jonay.kb.shopsystem.service;

import org.springframework.stereotype.Service;

import es.jonay.kb.shopsystem.model.entities.User;
import es.jonay.kb.shopsystem.model.repository.IUserRepository;

@Service
public class UserService {

    private IUserRepository userRepository;

    public UserService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findByEmail(String mail) {
        return userRepository.findByEmail(mail);
    }

}
