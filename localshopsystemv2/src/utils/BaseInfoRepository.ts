class BaseInfoRepositoryDevelopment {
  public static readonly BASE_URL = 'http://localhost:8080/kiosco/';
}
class BaseInfoRepositoryProduction {
  public static readonly BASE_URL = 'http://kioscobotanico.es:8080/kiosco/';

}
export const BaseInfoRepository = process.env.NODE_ENV === 'development' ? BaseInfoRepositoryDevelopment : BaseInfoRepositoryProduction;