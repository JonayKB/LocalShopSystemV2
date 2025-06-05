class BaseInfoRepository {
  public static getBaseUrl(ip: string | undefined): string {
    return `http://${ip}:8080/kiosco/`;
  }
}
export default BaseInfoRepository;
