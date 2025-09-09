package es.jonay.kb.shopsystem.controller;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import es.jonay.kb.shopsystem.api.dto.GraphDto;
import es.jonay.kb.shopsystem.model.repository.IGraphsRepository;

@Controller
public class GraphController {

    private IGraphsRepository graphsRepository;

    public IGraphsRepository getIGraphsRepository() {
        return this.graphsRepository;
    }

    @Autowired
    public void setIGraphsRepository(IGraphsRepository graphsRepository) {
        this.graphsRepository = graphsRepository;
    }

    public List<GraphDto<Long>> getTopSoldItems() {
        List<Object[]> queryResult = graphsRepository.topSoldItems();

        return queryResult.stream()
                .map(r -> new GraphDto<>((String) r[0], (Long) r[1]))
                .toList();
    }

    public List<GraphDto<Long>> getTradesCountByDay() {
        List<Object[]> queryResult = graphsRepository.countTradesByDay();

        return queryResult.stream()
                .map(r -> new GraphDto<>(((Date) r[0]).toString(), (Long) r[1]))
                .toList();
    }

    public Double getAverageTradeValue() {
        return graphsRepository.averageTradeValue();
    }

    public List<GraphDto<Long>> getIncomeByCategory() {
        List<Object[]> queryResult = graphsRepository.incomeByCategory();

        return queryResult.stream()
                .map(r -> new GraphDto<>((String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }

    public List<GraphDto<Long>> getStockByCategory() {
        List<Object[]> queryResult = graphsRepository.stockByCategory();

        return queryResult.stream()
                .map(r -> new GraphDto<>((String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }

    public List<String> getItemsIgnoringStock() {
        List<Object[]> queryResult = graphsRepository.itemsIgnoringStock();

        return queryResult.stream()
                .map(r -> (String) r[0])
                .toList();
    }

    public List<GraphDto<Double>> getTopProfitItems() {
        List<Object[]> queryResult = graphsRepository.topProfitItems();

        return queryResult.stream()
                .map(r -> new GraphDto<>((String) r[0], ((Number) r[1]).doubleValue()))
                .toList();
    }

    public List<GraphDto<Long>> getAccumulatedProfitOverTime() {
        List<Object[]> queryResult = graphsRepository.accumulatedProfitOverTime();

        return queryResult.stream()
                .map(r -> new GraphDto<>(((Date) r[0]).toString(), ((Number) r[1]).longValue()))
                .toList();
    }

}
