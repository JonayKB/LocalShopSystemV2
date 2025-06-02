package es.jonay.kb.shopsystem.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.CategoryDto;
import es.jonay.kb.shopsystem.controller.CategoryController;


@RestController
@RequestMapping("categories")
@CrossOrigin
public class CategoryService {
    CategoryController categoryController;

    public CategoryController getCategoryController() {
        return this.categoryController;
    }

    @Autowired
    public void setCategoryController(CategoryController categoryController) {
        this.categoryController = categoryController;
    }

    @GetMapping("/")
    public List<CategoryDto> getAll() {
        return categoryController.findAll();
    }

    @GetMapping("/{id}")
    public Optional<CategoryDto> getById(@PathVariable(name = "id") final Long id) {
        return categoryController.findById(id);
    }

    @PostMapping("/")
    public CategoryDto save(@RequestBody CategoryDto entity) {
        return categoryController.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable(name = "id") final Long id) {
        try {
            categoryController.deleteById(id);
            return ResponseEntity.noContent().build(); 
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
