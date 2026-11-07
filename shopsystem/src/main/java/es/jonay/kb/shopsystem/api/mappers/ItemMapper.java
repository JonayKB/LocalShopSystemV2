package es.jonay.kb.shopsystem.api.mappers;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.model.entities.Item;

/** Conversión Item -> ItemDto (price = precio de venta, net = coste). */
public interface ItemMapper {
    ItemMapper INSTANCE = new ItemMapper() {
    };

    default ItemDto toItemDto(Item item) {
        if (item == null) {
            return null;
        }
        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setCategoryId(item.getCategory() != null ? item.getCategory().getId() : null);
        dto.setImage(item.getImage());
        dto.setStock(item.getStock());
        dto.setBareMinimun(item.getBareMinimun());
        dto.setIgnoreStock(item.getIgnoreStock());
        dto.setPrice(item.getPrice());
        dto.setNet(item.getCost());
        return dto;
    }
}
