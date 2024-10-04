import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HistoryFileHandlerTests {
    private HistoryFileHandler historyFileHandler;

    @BeforeEach
    void setUp() {
        historyFileHandler = new HistoryFileHandler();
        historyFileHandler.CreateHistoryFile();
    }

    @Test
    void testIsFileExist_FileExists_ReturnsTrue() {
        File file = new File(historyFileHandler.fileName);
        assertTrue(file.exists());
    }

    @Test
    void testIsFileExist_FileDoesNotExist_ReturnsFalse() {
        File file = new File("nonexistentfile.json");
        assertFalse(file.exists());
    }
    @Test
    void testCreateHistoryFile_CreatesFileSuccessfully() {
        historyFileHandler.CreateHistoryFile();
        File file = new File(historyFileHandler.fileName);
        assertTrue(file.exists());
    }

    @Test
    void testUpdateWeather_UpdatesWeatherDataSuccessfully() {
        WeatherData weatherData = new WeatherData();
        historyFileHandler.UpdateWeather(weatherData);

        HistoryContent historyContent = historyFileHandler.LoadContent();
        assertEquals(1, historyContent.weatherData.size());
    }
    @Test
    void testUpdateWeather_UpdatesWeatherTwice() {
        WeatherData weatherData = new WeatherData();
        historyFileHandler.UpdateWeather(weatherData);
        historyFileHandler.UpdateWeather(weatherData);

        HistoryContent historyContent = historyFileHandler.LoadContent();
        assertEquals(2, historyContent.weatherData.size());
    }
    @Test
    void testCleaningHistoryData_RemovesOldWeatherData() {
        WeatherData oldData = new WeatherData(){};
        oldData.id = "id1";
        LocalDateTime now = LocalDateTime.now();
        oldData.lastUpdateTime = now.minusSeconds(31);

        WeatherData recentData = new WeatherData();
        recentData.id = "id2";
        recentData.lastUpdateTime = now;

        HistoryContent content = new HistoryContent(new ArrayList<>(), new LamportClock());
        content.weatherData.add(oldData);
        content.weatherData.add(recentData);
        historyFileHandler.WriteToFile(content);

        historyFileHandler.CleaningHistoryData();

        HistoryContent updatedContent = historyFileHandler.LoadContent();
        assertEquals(1, updatedContent.weatherData.size());
        assertEquals(recentData, updatedContent.weatherData.get(0));
    }

    @Test
    void testGetWeather_ReturnsCorrectJsonString() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenReturn("{\"weatherData\":[],\"clock\":{\"counter\":0}}");

        String jsonResult = historyFileHandler.GetWeather("all");
        assertEquals("{\"weatherData\":[],\"clock\":{\"counter\":0}}", jsonResult);
    }

    @Test
    void testClock_UpdatesClockIncrementCorrectly() {
        historyFileHandler.ClockIncrement(5);

        HistoryContent content = historyFileHandler.LoadContent();
        assertEquals(6, content.clock.counter);
    }
}
