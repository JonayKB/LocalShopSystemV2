package es.jonay.kb.shopsystem.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.Trade;

@Repository
public interface ITradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findAll();

    void deleteById(Long id);

    Optional<Trade> findById(Long id);

    Trade save(Trade entity);

    @Query("SELECT t FROM Trade t WHERE t.date >= :startDate AND t.date < :endDate")
    List<Trade> findAllTradesInRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    Page<Trade> findAll(Pageable pageable);


}
