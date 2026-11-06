package es.jonay.kb.shopsystem.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import es.jonay.kb.shopsystem.api.dto.CategoryDto;
import es.jonay.kb.shopsystem.api.mappers.CategoryMapper;
import es.jonay.kb.shopsystem.model.entities.Category;
import es.jonay.kb.shopsystem.model.repository.ICategoryRepository;

@Controller
public class CategoryController {
    private ICategoryRepository categoryRepository;
    private ImageUsageGuard imageUsageGuard;

    public ICategoryRepository getICategoryRepository() {
        return this.categoryRepository;
    }
    @Autowired
    public void setIItemRepository(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Autowired
    public void setImageUsageGuard(ImageUsageGuard imageUsageGuard) {
        this.imageUsageGuard = imageUsageGuard;
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

    /**
     * Crea (sin id o id inexistente) o edita una categoría: nombre, imagen e IGIC.
     * Al editar se conservan sus productos. La imagen no puede ser la de un producto
     * ni la de otra categoría.
     */
    @Transactional
    public CategoryDto save(CategoryDto categoryDto) {
        String name = categoryDto.getName() == null ? "" : categoryDto.getName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        BigDecimal rate = categoryDto.getIgicRate();
        if (rate != null && (rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException("El tipo de IGIC debe estar entre 0 y 100");
        }
        String image = categoryDto.getImage() == null || categoryDto.getImage().isBlank() ? null
                : categoryDto.getImage().trim();

        Category category = categoryDto.getId() == null ? null
                : categoryRepository.findById(categoryDto.getId()).orElse(null);
        boolean duplicatedName = category == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, category.getId());
        if (duplicatedName) {
            throw new IllegalArgumentException("Ya existe una categoría llamada \"" + name + "\"");
        }
        imageUsageGuard.checkForCategory(category == null ? null : category.getId(),
                category == null ? null : category.getImage(), image);

        if (category == null) {
            category = new Category(); // el id lo genera la base de datos (AUTO_INCREMENT)
        }
        category.setName(name);
        category.setImage(image);
        category.setIgicRate(rate);
        return CategoryMapper.INSTANCE.toCategoryDto(categoryRepository.save(category));
    }
    
    @Transactional
    public void deleteById(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new java.util.NoSuchElementException("Categoría no encontrada: " + id);
        }
        try {
            categoryRepository.deleteById(id);
            categoryRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new es.jonay.kb.shopsystem.api.exceptions.ConflictException(
                    "No se puede borrar la categoría: tiene productos. Muévelos antes a otra categoría.");
        }
    }
}
