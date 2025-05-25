package es.jonay.kb.shopsystem.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        return ItemMapper.INSTANCE.toItemDto(itemRepository.save(item));
    }

    public ItemDto update(ItemDto itemDto) {
        if (itemRepository.findById(itemDto.getId()) == null) {
            return itemDto;
        }
        Item item = ItemMapper.INSTANCE.toItem(itemDto);
        item.setCategory(categoryRepository.findById(itemDto.getCategoryId()).orElse(null));
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
}
