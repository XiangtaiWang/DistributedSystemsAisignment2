public class UpdateWeatherRequest{
    public WeatherData weatherData;
    public LamportClock lamportClock;
    public UpdateWeatherRequest(WeatherData weatherData, LamportClock lamportClock){
        this.weatherData = weatherData;
        this.lamportClock = lamportClock;
    }
    public UpdateWeatherRequest(){}
}
