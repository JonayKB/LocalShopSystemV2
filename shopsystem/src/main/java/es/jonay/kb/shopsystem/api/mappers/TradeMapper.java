package es.jonay.kb.shopsystem.api.mappers;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.api.dto.TradeLineDto;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.PaymentMethod;
import es.jonay.kb.shopsystem.model.entities.Trade;
import es.jonay.kb.shopsystem.model.entities.TradeLine;

@Mapper(componentModel = "spring")
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
        dto.setPaymentMethod(trade.getPaymentMethod() != null ? trade.getPaymentMethod().name() : PaymentMethod.CASH.name());
        dto.setCancelledAt(trade.getCancelledAt());
        dto.setCancellationReason(trade.getCancellationReason());
        dto.setTotalBase(trade.getTotalBase());
        dto.setTotalTax(trade.getTotalTax());
        dto.setTotal(trade.getTotal());
        for (TradeLine line : trade.getLines()) {
            dto.getLines().add(toLineDto(line));
        }

        // Solo ventas muy antiguas sin líneas (tabla trades_items). Las nuevas usan "lines".
        List<ItemDto> itemDtos = new ArrayList<>();
        if (trade.getLines() == null || trade.getLines().isEmpty()) {
            for (Item item : trade.getItems()) {
                itemDtos.add(ItemMapper.INSTANCE.toItemDto(item));
            }
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
        dto.setUnitCost(line.getUnitCost());
        dto.setCategoryName(line.getCategoryName());
        return dto;
    }

}
