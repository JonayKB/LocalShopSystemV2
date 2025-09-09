package es.jonay.kb.shopsystem.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.GraphDto;
import es.jonay.kb.shopsystem.controller.GraphController;

@RestController
@RequestMapping("graphs")
@CrossOrigin
public class GraphService {

    private GraphController graphController;

    public GraphController getGraphController() {
        return this.graphController;
    }

    @Autowired
    public void setGraphController(GraphController graphController) {
        this.graphController = graphController;
    }

    @Cacheable("top-sold-items")
    @GetMapping("top-sold-items")
    public List<GraphDto<Long>> getTopSoldItems() {
        return graphController.getTopSoldItems();
    }

    @Cacheable("trades-count-by-day")
    @GetMapping("trades-count-by-day")
    public List<GraphDto<Long>> getTradesCountByDay() {
        return graphController.getTradesCountByDay();
    }

    @Cacheable("average-trade-value")
    @GetMapping("average-trade-value")
    public Double getAverageTradeValue() {
        return graphController.getAverageTradeValue();
    }

    @Cacheable("income-by-category")
    @GetMapping("income-by-category")
    public List<GraphDto<Long>> getIncomeByCategory() {
        return graphController.getIncomeByCategory();
    }

    @Cacheable("stock-by-category")
    @GetMapping("stock-by-category")
    public List<GraphDto<Long>> getStockByCategory() {
        return graphController.getStockByCategory();
    }

    @Cacheable("items-ignored-stock")
    @GetMapping("items-ignored-stock")
    public List<String> getItemsIgnoringStock() {
        return graphController.getItemsIgnoringStock();
    }

    @Cacheable("top-profit-items")
    @GetMapping("top-profit-items")
    public List<GraphDto<Double>> getTopProfitItems() {
        return graphController.getTopProfitItems();
    }

    @Cacheable("accumulated-profit-overtime")
    @GetMapping("accumulated-profit-overtime")
    public List<GraphDto<Long>> getAccumulatedProfitOverTime() {
        return graphController.getAccumulatedProfitOverTime();
    }
}