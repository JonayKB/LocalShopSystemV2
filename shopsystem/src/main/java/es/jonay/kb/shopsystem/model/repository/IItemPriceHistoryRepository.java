package es.jonay.kb.shopsystem.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.ItemPriceChange;

@Repository
public interface IItemPriceHistoryRepository extends JpaRepository<ItemPriceChange, Long> {

    /** Historial de un producto, del cambio más reciente al más antiguo. */
    List<ItemPriceChange> findByItemIdOrderByValidFromDescIdDesc(Long itemId);
}
