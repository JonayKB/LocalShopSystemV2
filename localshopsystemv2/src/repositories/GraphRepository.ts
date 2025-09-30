import axios from "axios";
import {BaseInfoRepository} from "../utils/BaseInfoRepository";
import GraphData from "../models/Graph";

class GraphRepository {

    private static readonly GRAPH_PREFIX = "graphs/";

    // VENTAS
    async getTradeCountByDay(token: string | null) {
        const response = await axios.get(
            BaseInfoRepository.BASE_URL + GraphRepository.GRAPH_PREFIX + "trades-count-by-day",
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            }
        );
        const graphData = {
            labels: response.data.map((item: any) => item.labels),
            data: response.data.map((item: any) => item.data),
        };
        return graphData as GraphData;
    }

    async getTopSoldItems(token: string | null) {
        const response = await axios.get(
            BaseInfoRepository.BASE_URL + GraphRepository.GRAPH_PREFIX + "top-sold-items",
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            }
        );
        const graphData = {
            labels: response.data.map((item: any) => item.labels),
            data: response.data.map((item: any) => item.data),
        };
        return graphData as GraphData;
    }

    async getAverageTradeValue(token: string | null) {
        const response = await axios.get(
            BaseInfoRepository.BASE_URL + GraphRepository.GRAPH_PREFIX + "average-trade-value",
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            }
        );
        const graphData = {
            labels: ["Media valor de compra"],
            data: [response.data],
        };
        return graphData as GraphData;
    }

    async getIncomeByCategory(token: string | null) {
        const response = await axios.get(
            BaseInfoRepository.BASE_URL + GraphRepository.GRAPH_PREFIX + "income-by-category",
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            }
        );
        const graphData = {
            labels: response.data.map((item: any) => item.labels),
            data: response.data.map((item: any) => item.data),
        };
        return graphData as GraphData;
    }

    // STOCK
    async getStockByCategory(token: string | null) {
        const response = await axios.get(
            BaseInfoRepository.BASE_URL + GraphRepository.GRAPH_PREFIX + "stock-by-category",
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            }
        );
        const graphData = {
            labels: response.data.map((item: any) => item.labels),
            data: response.data.map((item: any) => item.data),
        };
        return graphData as GraphData;
    }
    async getItemsIgnoringStock(token: string | null) {
        const response = await axios.get(
            BaseInfoRepository.BASE_URL + GraphRepository.GRAPH_PREFIX + "items-ignored-stock",
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            }
        );
        const graphData = {
            labels: response.data,
            data: response.data.map(() => 1),
        };
        return graphData as GraphData;
    }

    // ECONOMIA

    async getTopProfitItems(token: string | null) {
        const response = await axios.get(
            BaseInfoRepository.BASE_URL + GraphRepository.GRAPH_PREFIX + "top-profit-items",
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            }
        );
        const graphData = {
            labels: response.data.map((item: any) => item.labels),
            data: response.data.map((item: any) => item.data),
        };
        return graphData as GraphData;
    }

    async getAccumulatedProfitOvertime(token: string | null) {
        const response = await axios.get(
            BaseInfoRepository.BASE_URL + GraphRepository.GRAPH_PREFIX + "accumulated-profit-overtime",
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            }
        );
        const graphData = {
            labels: response.data.map((item: any) => item.labels),
            data: response.data.map((item: any) => item.data),
        };
        return graphData as GraphData;
    }

}


export default GraphRepository;