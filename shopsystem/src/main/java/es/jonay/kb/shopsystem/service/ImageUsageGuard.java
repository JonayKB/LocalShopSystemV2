package es.jonay.kb.shopsystem.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import es.jonay.kb.shopsystem.api.exceptions.ImageInUseException;
import es.jonay.kb.shopsystem.model.repository.ICategoryRepository;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;

/**
 * Garantiza que una imagen pertenece a un único producto o categoría: un producto
 * no puede tener la foto de una categoría (ni de otro producto) y viceversa.
 * Solo se comprueba cuando la imagen cambia, para no bloquear la edición de
 * registros antiguos que ya compartieran foto.
 */
@Component
public class ImageUsageGuard {
    private final IItemRepository itemRepository;
    private final ICategoryRepository categoryRepository;

    public ImageUsageGuard(IItemRepository itemRepository, ICategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    public void checkForItem(Long itemId, String currentImage, String newImage) {
        if (isBlank(newImage) || Objects.equals(currentImage, newImage)) {
            return;
        }
        if (categoryRepository.existsByImage(newImage)) {
            throw new ImageInUseException("La imagen ya la usa una categoría. Sube una foto distinta para el producto.");
        }
        boolean usedByOtherItem = itemId == null
                ? itemRepository.existsByImage(newImage)
                : itemRepository.existsByImageAndIdNot(newImage, itemId);
        if (usedByOtherItem) {
            throw new ImageInUseException("La imagen ya la usa otro producto. Sube una foto distinta.");
        }
    }

    public void checkForCategory(Long categoryId, String currentImage, String newImage) {
        if (isBlank(newImage) || Objects.equals(currentImage, newImage)) {
            return;
        }
        if (itemRepository.existsByImage(newImage)) {
            throw new ImageInUseException("La imagen ya la usa un producto. Sube una foto distinta para la categoría.");
        }
        boolean usedByOtherCategory = categoryId == null
                ? categoryRepository.existsByImage(newImage)
                : categoryRepository.existsByImageAndIdNot(newImage, categoryId);
        if (usedByOtherCategory) {
            throw new ImageInUseException("La imagen ya la usa otra categoría. Sube una foto distinta.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
