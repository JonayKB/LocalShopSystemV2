package es.jonay.kb.shopsystem.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.Item;

@Repository
public interface IItemRepository extends JpaRepository<Item, Long> {
    List<Item> findAll();

    void deleteById(Long id);

    Optional<Item> findById(Long id);

    Item save(Item entity);

    List<Item> findByCategoryId(Long categoryId);

    List<Item> findByTradesId(Long tradesId);

    Page<Item> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Item> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String name, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.ignoreStock = false AND i.stock <= 0")
    List<Item> getOutOfStock();

    @Query("SELECT i FROM Item i WHERE i.ignoreStock = false AND i.stock < i.bareMinimun")
    List<Item> getUnderBareMinimun();
}
