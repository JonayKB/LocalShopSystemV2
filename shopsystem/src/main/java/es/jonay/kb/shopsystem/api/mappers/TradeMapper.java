package es.jonay.kb.shopsystem.api.mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.Trade;

@Mapper(uses = ItemMapper.class, componentModel = "spring")
public interface TradeMapper {
    TradeMapper INSTANCE = Mappers.getMapper(TradeMapper.class);

    default TradeDto toTradeDto(Trade trade) {
        TradeDto dto = new TradeDto();
        dto.setId(trade.getId());
        dto.setDate(trade.getDate());

        List<ItemDto> itemDtos = new ArrayList<>();
        LocalDateTime date = trade.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        for (Item item : trade.getItems()) {
            ItemDto dtoItem = new ItemDto();
            dtoItem.setId(item.getId());
            dtoItem.setName(item.getName());

            SortedMap<LocalDateTime, Double> history = item.getPriceHistory();
            SortedMap<LocalDateTime, Double> head = history.headMap(date);

            if (!head.isEmpty()) {
                dtoItem.setPrice(head.get(head.lastKey()));
            } else {
                dtoItem.setPrice(history.get(history.firstKey()));
            }

            SortedMap<LocalDateTime, Double> netHistory = item.getNetHistory();
            SortedMap<LocalDateTime, Double> netHead = netHistory.headMap(date);
            if (!netHead.isEmpty()) {
                dtoItem.setNet(netHead.get(netHead.lastKey()));
            } else {
                if(!netHistory.isEmpty()) {
                    dtoItem.setNet(netHistory.get(netHistory.firstKey()));
                } else {
                    dtoItem.setNet(0.0);
                }
            }

            dtoItem.setCategoryId(item.getCategory().getId());
            dtoItem.setImage(item.getImage());
            dtoItem.setStock(item.getStock());
            dtoItem.setBareMinimun(item.getBareMinimun());

            dtoItem.setIgnoreStock(item.getIgnoreStock());

            itemDtos.add(dtoItem);
        }

        dto.setItems(itemDtos);
        return dto;
    }

    @Mapping(target = "id", ignore = true)
    public Trade toTrade(TradeDto tradeDto);

}
