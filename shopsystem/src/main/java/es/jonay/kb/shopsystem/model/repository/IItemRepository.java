package es.jonay.kb.shopsystem.model.repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.Item;

@Repository
public interface IItemRepository extends JpaRepository<Item, Long> {
    List<Item> findAll();

    void deleteById(Long id);

    Optional<Item> findById(Long id);

    Item save(Item entity);

    List<Item> findByCategoryId(Long categoryId);

    boolean existsByImage(String image);

    boolean existsByImageAndIdNot(String image, Long id);

    List<Item> findByTradesId(Long tradesId);

    Page<Item> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Item> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String name, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.ignoreStock = false AND i.stock <= 0")
    List<Item> getOutOfStock();

    @Query("SELECT i FROM Item i WHERE i.ignoreStock = false AND i.stock < i.bareMinimun AND i.bareMinimun > 0")
    List<Item> getUnderBareMinimun();

    /**
     * Bloquea la fila del producto hasta el final de la transacción (SELECT ... FOR UPDATE):
     * dos cajas que venden el mismo producto a la vez no pierden descuentos de stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.category")
    List<Item> findAllWithCategory();
}
