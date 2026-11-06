import axios from "axios";
import { BaseInfoRepository } from "../utils/BaseInfoRepository";
import {
  Bucket,
  CategoryShare,
  Granularity,
  Inventory,
  ItemRank,
  PaymentShare,
  RankMetric,
  Summary,
  TimePoint,
} from "../models/Analytics";

/** Rango de fechas (yyyy-mm-dd, ambas incluidas). */
export type DateRange = { from: string; to: string };

class GraphRepository {
  private static readonly PREFIX = "graphs/";

  private async get<T>(path: string, token: string | null, params: Record<string, unknown> = {}): Promise<T> {
    const response = await axios.get<T>(BaseInfoRepository.BASE_URL + GraphRepository.PREFIX + path, {
      headers: { Authorization: `Bearer ${token}` },
      params,
    });
    return response.data;
  }

  summary(range: DateRange, token: string | null) {
    return this.get<Summary>("summary", token, range);
  }

  salesOverTime(range: DateRange, granularity: Granularity, token: string | null) {
    return this.get<TimePoint[]>("sales-over-time", token, { ...range, granularity });
  }

  topItems(range: DateRange, metric: RankMetric, limit: number, token: string | null) {
    return this.get<ItemRank[]>("top-items", token, { ...range, metric, limit });
  }

  byCategory(range: DateRange, token: string | null) {
    return this.get<CategoryShare[]>("by-category", token, range);
  }

  byPaymentMethod(range: DateRange, token: string | null) {
    return this.get<PaymentShare[]>("by-payment-method", token, range);
  }

  byHour(range: DateRange, token: string | null) {
    return this.get<Bucket[]>("by-hour", token, range);
  }

  byWeekday(range: DateRange, token: string | null) {
    return this.get<Bucket[]>("by-weekday", token, range);
  }

  inventory(token: string | null) {
    return this.get<Inventory>("inventory", token);
  }
}

export default GraphRepository;
