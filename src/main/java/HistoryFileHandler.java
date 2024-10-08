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
        // try create file, if exception then write empty
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
        // read file, insert new data, then write back
        HistoryContent historyContent = LoadContent();
        data.setLastUpdateTime();
        historyContent.weatherData.add(0, data);
        WriteToFile(historyContent);
        System.out.println("Successfully update History File");
    }

    protected void WriteToFile(HistoryContent historyContent) {
        //write history content to file
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
        // remove records which index more than 20, remove if an ID hasn't updated recent 30s
        // clock increment since it is an action will impact data
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
        // generate a hash map contain last update time by each id
        HashMap<String, LocalDateTime> updateTimeTable = new HashMap<>();{}
        for (WeatherData weatherData : historyContent.weatherData) {
            if (!updateTimeTable.containsKey(weatherData.id) || weatherData.lastUpdateTime.isAfter(updateTimeTable.get(weatherData.id))) {
                updateTimeTable.put(weatherData.id, weatherData.lastUpdateTime);
            }
        }
        return updateTimeTable;
    }

    public String GetWeather(String id) {
        //if id != all, find specific weather data, else return all data
        String data;
        HistoryContent historyContent = LoadContent();
        ObjectMapper objectMapper = AggregationServer.getObjectMapper();

        if(!id.equals("all" )){
            HistoryContent weatherInfo = new HistoryContent();
            weatherInfo.clock = historyContent.clock;
            for (WeatherData weather : historyContent.weatherData) {
                if (weather.id.equals(id)) {
                    weatherInfo.weatherData = new ArrayList<>(){};
                    weatherInfo.weatherData.add(weather);
                    break;
                }
            }

            try {
                data = objectMapper.writeValueAsString(weatherInfo);
            } catch (JsonProcessingException e) {
                System.out.println("An error occurred.");
                throw new RuntimeException(e);
            }
        }else {
            try {
                data = objectMapper.writeValueAsString(historyContent);
            } catch (JsonProcessingException e) {
                System.out.println("An error occurred. All");
                throw new RuntimeException(e);
            }
        }
        return data;
    }

    private HistoryContent ConvertToHistoryContentObj(String data) {
        // convert json str to HistoryContentObj
        ObjectMapper objectMapper = AggregationServer.getObjectMapper();
        HistoryContent history;
        try {
            history = objectMapper.readValue(data, HistoryContent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return history;
    }

    private String ReadHistoryFile() {
        // read file content as string
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
        // read file, convert to HistoryContent, then return
        // if format or sth wrong, re-initialize data
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
        // had action, compare incoming lamportclock counter then +1
        HistoryContent historyContent = LoadContent();
        historyContent.clock.ReceivedAction(incomingCounter);
        WriteToFile(historyContent);
    }
}
