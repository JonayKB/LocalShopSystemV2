package es.jonay.kb.shopsystem.api.mappers;



import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import es.jonay.kb.shopsystem.api.dto.CategoryDto;
import es.jonay.kb.shopsystem.model.entities.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);
    public CategoryDto toCategoryDto(Category category);

    public Category toCategory(CategoryDto categoryDto);
}
