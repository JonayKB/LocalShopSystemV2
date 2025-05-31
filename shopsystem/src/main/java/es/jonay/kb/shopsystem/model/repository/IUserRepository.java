package es.jonay.kb.shopsystem.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.User;



@Repository
public interface IUserRepository extends JpaRepository<User, Long>{
    User findByEmail(String mail);
    
}
