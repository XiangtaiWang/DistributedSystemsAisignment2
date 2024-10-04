public class WeatherRequest {
    public WeatherData weatherData;
    public LamportClock lamportClock;
    public WeatherRequest(WeatherData weatherData, LamportClock lamportClock){
        this.weatherData = weatherData;
        this.lamportClock = lamportClock;
    }
    public WeatherRequest(){}
}
