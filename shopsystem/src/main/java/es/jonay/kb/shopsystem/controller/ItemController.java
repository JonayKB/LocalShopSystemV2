package es.jonay.kb.shopsystem.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.mappers.ItemMapper;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.repository.ICategoryRepository;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;

@Controller
public class ItemController {
    private IItemRepository itemRepository;
    private ICategoryRepository categoryRepository;

    public IItemRepository getIItemRepository() {
        return this.itemRepository;
    }

    @Autowired
    public void setIItemRepository(IItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Autowired
    public void setIICategoryRepository(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public ICategoryRepository getICategoryRepository() {
        return this.categoryRepository;
    }

    public List<ItemDto> findAll() {
        List<Item> items = itemRepository.findAll();
        List<ItemDto> result = new ArrayList<ItemDto>();
        for (Item item : items) {
            result.add(ItemMapper.INSTANCE.toItemDto(item));
        }
        return result;
    }

    public Optional<ItemDto> findById(Long id) {
        Optional<Item> item = itemRepository.findById(id);
        return item.map(ItemMapper.INSTANCE::toItemDto);
    }

    public List<ItemDto> findByCategoryId(Long categoryId) {
        List<ItemDto> itemDtos = new ArrayList<>();
        for (Item item : itemRepository.findByCategoryId(categoryId)) {
            itemDtos.add(ItemMapper.INSTANCE.toItemDto(item));
        }
        return itemDtos;
    }

    public List<ItemDto> findByTradeId(Long tradeId) {
        List<ItemDto> itemDtos = new ArrayList<>();
        for (Item item : itemRepository.findByTradesId(tradeId)) {
            itemDtos.add(ItemMapper.INSTANCE.toItemDto(item));
        }
        return itemDtos;
    }

    public ItemDto save(ItemDto itemDto) {
        Item item = ItemMapper.INSTANCE.toItem(itemDto);
        item.setCategory(categoryRepository.findById(itemDto.getCategoryId()).orElse(null));
        SortedMap<LocalDateTime, Double> priceHistory = new TreeMap<>();

        priceHistory.put(LocalDateTime.now(), itemDto.getPrice());

        item.setPriceHistory(priceHistory);

        return ItemMapper.INSTANCE.toItemDto(itemRepository.save(item));
    }

    public ItemDto update(ItemDto itemDto) {
        if (!itemRepository.existsById(itemDto.getId())) {
            return itemDto;
        }
        Item item = itemRepository.findById(itemDto.getId()).get();
        item.setCategory(categoryRepository.findById(itemDto.getCategoryId()).orElse(null));
        item.setBareMinimun(itemDto.getBareMinimun());
        item.setIgnoreStock(itemDto.getIgnoreStock());
        item.setImage(itemDto.getImage());
        item.setName(itemDto.getName());
        item.setStock(itemDto.getStock());
        item.setNet(itemDto.getNet());
        if (item.getPriceHistory().isEmpty()
                || item.getPriceHistory().get(item.getPriceHistory().lastKey()) != itemDto.getPrice()) {
            item.getPriceHistory().put(LocalDateTime.now(), itemDto.getPrice());

        }
        return ItemMapper.INSTANCE.toItemDto(itemRepository.save(item));
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    public Page<ItemDto> findByNameContainingIgnoreCase(String name, Integer page, Integer size, String sortBy,
            boolean ascending) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "name";
        }

        return itemRepository
                .findByNameContainingIgnoreCase(name, PageRequest.of(page, size,
                        ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending()))
                .map(ItemMapper.INSTANCE::toItemDto);
    }

    public Item removeStock(Long id, Integer amount) {
        Optional<Item> itemOptional = itemRepository.findById(id);
        if (itemOptional.isPresent()) {
            Item item = itemOptional.get();
            item.setStock(item.getStock() - amount);
            item = itemRepository.save(item);
            if (item != null) {
                return item;
            }
            return item;
        }
        return null;
    }

    public List<Item> removeStock(List<Item> items, Integer amount) {
        List<Item> result = new ArrayList<>();
        for (Item item : items) {
            result.add(removeStock(item.getId(), amount));

        }
        return result;
    }

    public Item addStock(Long id, Integer amount) {
        Optional<Item> itemOptional = itemRepository.findById(id);
        if (itemOptional.isPresent()) {
            Item item = itemOptional.get();
            item.setStock(item.getStock() + amount);
            item = itemRepository.save(item);
            if (item != null) {
                return item;
            }
            return item;
        }
        return null;
    }

    public List<Item> addStock(List<Item> items, Integer amount) {
        List<Item> result = new ArrayList<>();
        for (Item item : items) {
            result.add(addStock(item.getId(), amount));

        }
        return result;
    }

    public Page<ItemDto> findByCategoryId(String name, int page, int size, String sortBy, boolean ascending,
            Long categoryId) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "name";
        }
        Pageable pageable = PageRequest.of(page, size, ascending ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending());
        if (categoryId == null) {
            return itemRepository.findByNameContainingIgnoreCase(name, pageable)
                    .map(ItemMapper.INSTANCE::toItemDto);
        } else {
            return itemRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, name, pageable)
                    .map(ItemMapper.INSTANCE::toItemDto);
        }
    }

    public List<Item> getOutOfStockItems() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'GetOutOfStockItems'");
    }
}
