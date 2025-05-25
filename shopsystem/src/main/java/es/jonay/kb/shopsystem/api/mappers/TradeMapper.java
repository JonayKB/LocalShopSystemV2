package es.jonay.kb.shopsystem.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.model.entities.Trade;
@Mapper(uses = ItemMapper.class)
public interface TradeMapper {
    TradeMapper INSTANCE = Mappers.getMapper(TradeMapper.class);
    public TradeDto toTradeDto(Trade trade);
    
    @Mapping(target = "id", ignore = true)
    public Trade toTrade(TradeDto tradeDto);
}
