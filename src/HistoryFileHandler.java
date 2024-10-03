import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HistoryFileHandler {
    public String fileName = "history.json";

    public boolean IsFileExist() {
        File f = new File(fileName);
        return f.exists() && !f.isDirectory();
    }

    public void CreateHistoryFile() {
        File file = new File(fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        WriteEmptyHistoryContent();
        System.out.println("Successfully Initialize HistoryFile.");
    }

    public void UpdateWeather(WeatherData data) {
        HistoryContent historyContent = LoadContent();
        data.setLastUpdateTime();
        historyContent.weatherData.add(0, data);
        WriteToFile(historyContent);
        System.out.println("Successfully update History File");
    }

    protected void WriteToFile(HistoryContent historyContent) {
        try {
            FileWriter myWriter = new FileWriter(fileName);
            ObjectMapper objectMapper = AggregationServer.getObjectMapper();
            objectMapper.writeValue(myWriter, historyContent);
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void CleaningHistoryData() {
        HistoryContent historyContent = LoadContent();

        if (historyContent.weatherData.size() > 20) {
            historyContent.weatherData = historyContent.weatherData.subList(0, 20);
        }

        HashMap<String, LocalDateTime> lastUpdateTimeById = GetLastUpdateTimeById(historyContent);
        LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);
        for (Map.Entry<String, LocalDateTime> entry : lastUpdateTimeById.entrySet()) {
            if (entry.getValue().isBefore(thirtySecondsAgo)) {
                historyContent.weatherData.removeIf(weatherData -> weatherData.id.equals(entry.getKey()));
            }
        }

        historyContent.clock.ReceivedAction(historyContent.clock.counter);
        WriteToFile(historyContent);
        System.out.println("Successfully clean Old Data.");
    }

    private HashMap<String, LocalDateTime> GetLastUpdateTimeById(HistoryContent historyContent) {
        HashMap<String, LocalDateTime> updateTime = new HashMap<>();
        {
        }
        for (WeatherData weatherData : historyContent.weatherData) {
            if (!updateTime.containsKey(weatherData.id) || weatherData.lastUpdateTime.isAfter(updateTime.get(weatherData.id))) {
                updateTime.put(weatherData.id, weatherData.lastUpdateTime);
            }
        }
        return updateTime;
    }

    public String GetWeather() {
        HistoryContent historyContent = LoadContent();
        ObjectMapper objectMapper = AggregationServer.getObjectMapper();
        try {
            return objectMapper.writeValueAsString(historyContent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private HistoryContent ConvertToHistoryContentObj(String data) {
        ObjectMapper objectMapper = AggregationServer.getObjectMapper();
        HistoryContent history;
        try {
//                history = objectMapper.readValue(data, new TypeReference<List<WeatherData>>(){});
            history = objectMapper.readValue(data, HistoryContent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return history;
    }

    private String ReadHistoryFile() {
        try {
            StringBuilder data = new StringBuilder();
            File myObj = new File(fileName);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                data.append(myReader.nextLine());
            }
            myReader.close();
            return data.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public HistoryContent LoadContent() {
        try {
            if (IsFileExist()) {
                String content = ReadHistoryFile();
                HistoryContent historyContent = ConvertToHistoryContentObj(content);
                return historyContent;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        WriteEmptyHistoryContent();
        return LoadContent();
    }

    private void WriteEmptyHistoryContent() {
        HistoryContent historyContent = new HistoryContent(new ArrayList<>(), new LamportClock());
        WriteToFile(historyContent);
    }

    public void ClockIncrement(int incomingCounter) {
        HistoryContent historyContent = LoadContent();
        historyContent.clock.ReceivedAction(incomingCounter);
        WriteToFile(historyContent);
    }
}
