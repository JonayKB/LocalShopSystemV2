package es.jonay.kb.shopsystem.controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.api.mappers.ItemMapper;
import es.jonay.kb.shopsystem.api.mappers.TradeMapper;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.Trade;
import es.jonay.kb.shopsystem.model.repository.ICategoryRepository;
import es.jonay.kb.shopsystem.model.repository.ITradeRepository;

@Controller
public class TradeController {
    private ITradeRepository tradeRepository;
    private ICategoryRepository categoryRepository;
    private ItemController itemController;

    @Autowired
    public void setIICategoryRepository(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public ICategoryRepository getICategoryRepository() {
        return this.categoryRepository;
    }

    public ITradeRepository getITradeRepository() {
        return this.tradeRepository;
    }

    @Autowired
    public void setITradeRepository(ITradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    @Autowired
    public void setItemController(ItemController itemController) {
        this.itemController = itemController;
    }

    public List<TradeDto> findAll() {
        List<Trade> trades = tradeRepository.findAll();
        List<TradeDto> result = new ArrayList<TradeDto>();
        for (Trade trade : trades) {
            result.add(TradeMapper.INSTANCE.toTradeDto(trade));
        }
        return result;
    }

    public Optional<TradeDto> findById(Long id) {
        Optional<Trade> trade = tradeRepository.findById(id);
        return trade.map(TradeMapper.INSTANCE::toTradeDto);
    }

    public TradeDto save(TradeDto tradeDto) {
        Trade trade = TradeMapper.INSTANCE.toTrade(tradeDto);

        tradeDto = TradeMapper.INSTANCE.toTradeDto(tradeRepository.save(trade));
        if (tradeDto != null) {
            // REMOVE STOCK
            itemController.removeStock(trade.getItems(), 1);
            return tradeDto;
        }
        return null;

    }

    public TradeDto saveList(List<ItemDto> items) {

        List<Item> itemList = new ArrayList<>();
        for (ItemDto itemDto : items) {
            Item item = ItemMapper.INSTANCE.toItem(itemDto);
            item.setCategory(categoryRepository.findById(itemDto.getCategoryId()).get());
            itemList.add(item);
            itemController.removeStock(item.getId(), 1);
        }
        Trade trade = new Trade();
        trade.setDate(new Date());
        trade.setItems(itemList);
        return TradeMapper.INSTANCE.toTradeDto(tradeRepository.save(trade));
    }

    public List<TradeDto> findTradesInRange(LocalDateTime startDate, LocalDateTime endDate) {

        // Ensure that the time range is correctly defined
        if (startDate == null) {
            startDate = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIDNIGHT);
        }

        if (endDate == null) {
            endDate = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MAX);
        }
        List<TradeDto> tradesDtoList = new ArrayList<TradeDto>();
        for (Trade trade : tradeRepository.findAllTradesInRange(startDate, endDate)) {
            tradesDtoList.add(TradeMapper.INSTANCE.toTradeDto(trade));
        }
        return tradesDtoList;
    }

    public void deleteById(Long id) {
        tradeRepository.deleteById(id);
    }

    public Page<TradeDto> getPage(int page, int size) {
        Page<Trade> trades = tradeRepository.findAll(PageRequest.of(page, size).withSort(Sort.by("date").descending()));
        return trades.map(TradeMapper.INSTANCE::toTradeDto);
    }

}
