package es.jonay.kb.shopsystem.api.mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.api.dto.TradeLineDto;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.Trade;
import es.jonay.kb.shopsystem.model.entities.TradeLine;

@Mapper(uses = ItemMapper.class, componentModel = "spring")
public interface TradeMapper {
    TradeMapper INSTANCE = Mappers.getMapper(TradeMapper.class);

    default TradeDto toTradeDto(Trade trade) {
        TradeDto dto = new TradeDto();
        dto.setId(trade.getId());
        dto.setDate(trade.getDate());
        dto.setSeries(trade.getSeries());
        dto.setNumber(trade.getNumber());
        if (trade.getSeries() != null && trade.getNumber() != null) {
            dto.setInvoiceNumber(String.format("%s-%06d", trade.getSeries(), trade.getNumber()));
        }
        dto.setStatus(trade.getStatus() != null ? trade.getStatus().name() : null);
        dto.setCancelledAt(trade.getCancelledAt());
        dto.setCancellationReason(trade.getCancellationReason());
        dto.setTotalBase(trade.getTotalBase());
        dto.setTotalTax(trade.getTotalTax());
        dto.setTotal(trade.getTotal());
        for (TradeLine line : trade.getLines()) {
            dto.getLines().add(toLineDto(line));
        }

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

    default TradeLineDto toLineDto(TradeLine line) {
        TradeLineDto dto = new TradeLineDto();
        dto.setItemId(line.getItemId());
        dto.setDescription(line.getDescription());
        dto.setQuantity(line.getQuantity());
        dto.setUnitPrice(line.getUnitPrice());
        dto.setTaxRate(line.getTaxRate());
        dto.setTaxBase(line.getTaxBase());
        dto.setTaxAmount(line.getTaxAmount());
        dto.setLineTotal(line.getLineTotal());
        return dto;
    }

}
