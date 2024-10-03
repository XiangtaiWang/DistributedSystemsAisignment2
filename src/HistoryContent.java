import java.util.List;

public class HistoryContent {
    public List<WeatherData> weatherData;
    public LamportClock clock;

    public HistoryContent(List<WeatherData> records, LamportClock lamportClock) {
        weatherData = records;
        clock = lamportClock;
    }
    public HistoryContent(){}
}
