package ru.practicum.ewm.category.mapper;

import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;

public class CategoryMapper {
    public static Category toCategory(NewCategoryDto dto) {
        if (dto == null) {
            return null;
        }
        return Category.builder()
                .name(dto.getName())
                .build();
    }

    public static CategoryDto toCategoryDto(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
