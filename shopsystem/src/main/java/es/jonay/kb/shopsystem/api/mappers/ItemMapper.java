package es.jonay.kb.shopsystem.api.mappers;

import java.time.LocalDateTime;
import java.util.SortedMap;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.model.entities.Item;

@Mapper
public interface ItemMapper {
    ItemMapper INSTANCE = Mappers.getMapper(ItemMapper.class);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "item.priceHistory", target = "price", qualifiedByName = "getLatestPrice")
    @Mapping(source = "item.netHistory", target = "net", qualifiedByName = "getLatestNet")
    public ItemDto toItemDto(Item item);

    public Item toItem(ItemDto itemDto);

    @Named("getLatestPrice")
    public default Double getLatestPrice(SortedMap<LocalDateTime, Double> priceHistory) {
        if (priceHistory != null && !priceHistory.isEmpty())
            return priceHistory.get(priceHistory.lastKey());
        return 0.0;
    }

    @Named("getLatestNet")
    public default Double getLatestNet(SortedMap<LocalDateTime, Double> netHistory) {
        if (netHistory != null && !netHistory.isEmpty())
            return netHistory.get(netHistory.lastKey());
        return 0.0;
    }
}
