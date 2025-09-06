import axios from "axios";
import Item from "../models/Item";
import BaseInfoRepository from "../utils/BaseInfoRepository";

class StockRepository {
    async addStock(itemsToAdd: Map<Item, number>, token: string | null) {
        const body: Record<number, number> = {};
        itemsToAdd.forEach((quantity, item) => {
            body[item.id] = quantity;
        });

        const res = await axios.post(BaseInfoRepository.BASE_URL + 'stock/', body, {
            headers: {
                Authorization: `Bearer ${token}`
            },
            data: body
        });

        return res.data;
    }

    async removeStock(itemsToRemove: Map<Item, number>, token: string | null) {
        const body: Record<number, number> = {};
        itemsToRemove.forEach((quantity, item) => {
            body[item.id] = quantity;
        });

        const res = await axios.delete(BaseInfoRepository.BASE_URL + 'stock/', {
            headers: {
                Authorization: `Bearer ${token}`
            },
            data: body,
            responseType: 'blob'
        });

        const blob = new Blob([res.data], {
            type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        });
        console.log(blob);

        return blob;
    }

}

export default StockRepository;