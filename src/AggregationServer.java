import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

public class AggregationServer{
    private final int port = 4567;
    private Socket socket = null;
    private ServerSocket server = null;
    private LamportClock clock;
    public static void main(String[] args)  {
        AggregationServer aggregationServer = new AggregationServer();
    }
    public AggregationServer()
    {
        // todo: implement lamport clock and store it as well
        // todo: when initializing, restore clock
        HistoryFileHandler historyFileHandler = new HistoryFileHandler();
        RequestHandler requestHandler = new RequestHandler(historyFileHandler);
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");
            System.out.println("Waiting for a client ...");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        Timer timer = new Timer();
        timer.schedule(new RemoveTooOldDataTask(historyFileHandler), 0, 5000);

        while (true) {
            try {
                socket = server.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            new SocketThread(socket, requestHandler).start();
        }

    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    private LamportClock InitialIzeClock() {
        LamportClock clock = new LamportClock();
        return clock;
    }

    class RemoveTooOldDataTask extends TimerTask {
        private HistoryFileHandler historyFileHandler;
        public RemoveTooOldDataTask(HistoryFileHandler historyFileHandlerDI) {
            historyFileHandler = historyFileHandlerDI;
        }
        public void run() {
            historyFileHandler.DeleteOldData();
        }
    }
    class HistoryFileHandler{
        public String fileName = "history.json";
        private List<WeatherData> records;
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
            historyContent.weatherData.add(data);
            WriteToFile(historyContent);
            System.out.println("Successfully update History File");
        }

        private void WriteToFile(HistoryContent historyContent) {
            try {
                FileWriter myWriter = new FileWriter(fileName);
                ObjectMapper objectMapper = getObjectMapper();
                objectMapper.writeValue(myWriter, historyContent);
                myWriter.close();
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }

        public void DeleteOldData() {
            //todo: can only store up to 20 record, if a station not update in last 30s, delete as well

            HistoryContent historyContent = LoadContent();
            LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);
            historyContent.weatherData.removeIf(weatherData -> weatherData.lastUpdateTime.isBefore(thirtySecondsAgo));
            historyContent.clock.ReceivedAction(historyContent.clock.counter);
            WriteToFile(historyContent);
            System.out.println("Successfully remove Old Data.");
        }

        public String GetWeather() {
            HistoryContent historyContent = LoadContent();
            ObjectMapper objectMapper = getObjectMapper();
            try {
                return objectMapper.writeValueAsString(historyContent);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }

        private HistoryContent ConvertToHistoryContentObj(String data) {
            ObjectMapper objectMapper = getObjectMapper();
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
            }catch (Exception e)
            {
                e.printStackTrace();
            }
            WriteEmptyHistoryContent();
            return LoadContent();
        }

        private void WriteEmptyHistoryContent() {
            HistoryContent historyContent = new HistoryContent(new ArrayList<>(), new LamportClock());
            WriteToFile(historyContent);
        }

        public void UpdateClock(int incomingCounter) {
            HistoryContent historyContent = LoadContent();
            historyContent.clock.ReceivedAction(incomingCounter);
            WriteToFile(historyContent);
        }
    }

    class SocketThread extends Thread{
        private final Socket socket;
        private final RequestHandler requestHandler;
        public SocketThread(Socket clientSocket, RequestHandler requestHandlerDI) {
            this.socket = clientSocket;
            requestHandler = requestHandlerDI;
        }
        public void run() {
            InputStream inp = null;
            BufferedReader brinp = null;
            DataOutputStream out = null;

            try {
                inp = socket.getInputStream();
                brinp = new BufferedReader(new InputStreamReader(inp));
                out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                return;
            }
            String message;
            StringBuilder gatheredMessage =  new StringBuilder();
            while (true) {
                try {
                    message = brinp.readLine();
                    if ((message == null) || message.equalsIgnoreCase("done")) {
                        socket.close();
                        return;
                    } else {
                        gatheredMessage.append(message).append("\n");
                        if (message.startsWith(String.valueOf('{')) && message.endsWith("}")) {
                            String response = requestHandler.HandleRequest(gatheredMessage.toString());
                            out.writeBytes(response + "\n\r");
                            out.flush();
                            gatheredMessage.delete(0, gatheredMessage.length());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
    class RequestHandler{
        private final HistoryFileHandler historyFileHandler;
        public RequestHandler(HistoryFileHandler historyFileHandlerDI) {
            historyFileHandler = historyFileHandlerDI;
        }
        public String HandleRequest(String request) {
            String[] split = request.split("\n");

            String response;
            if (split[0].startsWith("PUT") ){
                System.out.println("Received PUT request");
                String body = split[split.length - 1];
                response = ProcessUpdateWeatherRequest(body);
            }
            else if (split[0].startsWith("GET") ){
                response = GetWeatherRequest();
            }
            else {
                response = BadRequest();
            }

            return response;
        }

        private String BadRequest() {
            CreateResponse(HttpStatus.HTTP_BAD_REQUEST, "{}");
            return null;
        }
        private String CreateResponse(HttpStatus httpStatus, String body) {
            int code;
            switch (httpStatus) {
                case HTTP_CREATED:
                    code = 201;
                    break;
                case HTTP_SUCCESS:
                    code = 200;
                    break;
                case HTTP_BAD_REQUEST:
                    code = 400;
                    break;
                case HTTP_INTERNAL_ERROR:
                    code = 500;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + httpStatus);
            }
            String status = code + " " + httpStatus.toString();
            String httpVersion = "HTTP/1.1";
            String contentType = "application/json";
            int contentLength = body.getBytes().length;

            StringBuilder response = new StringBuilder();
            response.append(httpVersion)
                    .append(" ")
                    .append(status)
                    .append("\r\n");


            response.append("Content-Type: ")
                    .append(contentType)
                    .append("\r\n");

            response.append("Content-Length: ")
                    .append(contentLength)
                    .append("\r\n");

            response.append("\r\n");
            response.append(body);

            return response.toString();
        }
        private String ProcessUpdateWeatherRequest(String body) {
            ObjectMapper objectMapper = getObjectMapper();
            UpdateWeatherRequest rq;
            String response;

            try {
                rq = objectMapper.readValue(body, UpdateWeatherRequest.class);
                if (!historyFileHandler.IsFileExist()){
                    System.out.println("file does not exist, initializing...");
                    historyFileHandler.CreateHistoryFile();
                    System.out.println("file initializing finished, weather updating...");
                    historyFileHandler.UpdateWeather(rq.weatherData);
                    historyFileHandler.UpdateClock(rq.lamportClock.counter);
                    HistoryContent historyContent = historyFileHandler.LoadContent();
                    response = CreateResponse(HttpStatus.HTTP_CREATED, objectMapper.writeValueAsString(historyContent.clock));

                }else{
                    System.out.println("weather updating...");
                    historyFileHandler.UpdateWeather(rq.weatherData);
                    historyFileHandler.UpdateClock(rq.lamportClock.counter);
                    HistoryContent historyContent = historyFileHandler.LoadContent();
                    response = CreateResponse(HttpStatus.HTTP_SUCCESS, objectMapper.writeValueAsString(historyContent.clock));
                }
            }catch(Exception e){
                response = CreateResponse(HttpStatus.HTTP_INTERNAL_ERROR, e.getMessage());
            }

            return response;
        }

        private String GetWeatherRequest() {
            String data = historyFileHandler.GetWeather();
            return CreateResponse(HttpStatus.HTTP_SUCCESS, data);
        }
    }
}

