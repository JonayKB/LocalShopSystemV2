package es.jonay.kb.shopsystem.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import es.jonay.kb.shopsystem.api.dto.CategoryDto;
import es.jonay.kb.shopsystem.api.mappers.CategoryMapper;
import es.jonay.kb.shopsystem.model.entities.Category;
import es.jonay.kb.shopsystem.model.repository.ICategoryRepository;

@Controller
public class CategoryController {
    private ICategoryRepository categoryRepository;

    public ICategoryRepository getICategoryRepository() {
        return this.categoryRepository;
    }
    @Autowired
    public void setIItemRepository(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDto> findAll(){
        List<Category> categories = categoryRepository.findAll();
        List<CategoryDto> result = new ArrayList<CategoryDto>();
        for(Category category : categories){
            result.add(CategoryMapper.INSTANCE.toCategoryDto(category));
        }
        return result;
    }

    public Optional<CategoryDto> findById(Long id){
        Optional<Category> category = categoryRepository.findById(id);
        return category.map(CategoryMapper.INSTANCE::toCategoryDto);
    }

    public CategoryDto save(CategoryDto categoryDto){
        Category category = CategoryMapper.INSTANCE.toCategory(categoryDto);
        return CategoryMapper.INSTANCE.toCategoryDto(categoryRepository.save(category));
    }
    
    public void deleteById(Long id){
        categoryRepository.deleteById(id);
    }
}
