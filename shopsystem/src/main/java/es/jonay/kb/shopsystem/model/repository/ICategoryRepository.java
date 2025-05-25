package es.jonay.kb.shopsystem.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.Category;

@Repository
public interface ICategoryRepository extends JpaRepository<Category,Long> {
    List<Category> findAll();

    void deleteById(Long id);

    Optional<Category> findById(Long id);

    Category save(Category entity);

}

