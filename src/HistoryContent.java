import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class HistoryContent {
    public List<WeatherData> weatherData;
    public LamportClock clock;
    private Map<String, LocalDateTime>UpdateTime;

    public HistoryContent(List<WeatherData> records, LamportClock lamportClock) {
        weatherData = records;
        clock = lamportClock;
    }
    public HistoryContent(){}
}
