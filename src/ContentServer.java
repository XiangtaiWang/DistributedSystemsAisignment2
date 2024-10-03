import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class ContentServer{
    private BufferedReader brinp;
    // todo: lamport clock
    private String address = "127.0.0.1";
    private int port = 4567;
    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;

    public ContentServer(){
        try {
            var socket = new Socket(address, port);
            System.out.println("Connected");
            InputStream inp = socket.getInputStream();
            brinp = new BufferedReader(new InputStreamReader(inp));
            input = new DataInputStream(System.in);
            out = new DataOutputStream(
                    socket.getOutputStream());
        }
        catch (IOException i) {
            System.out.println(i);
            return;
        }
        LamportClock clock = new LamportClock();
        Timer timer = new Timer();
        timer.schedule(new UpdateWeatherTask(out, clock), 0, 5000);
        new DisplayResponseThread(brinp, clock).start();
        String line = "";
        while (!line.equals("done")) {
            try {
                line = input.readLine();
                out.writeBytes(line+"\n");
            }
            catch (IOException i) {
                System.out.println(i);
            }
        }
        try {
            input.close();
            out.close();
            socket.close();
        }
        catch (IOException i) {
            System.out.println(i);
        }
    }

    private String ConvertObjectToJson(Object data) {
        ObjectMapper om = new ObjectMapper();
        String json;
        try {
            json = om.writeValueAsString(data);
            return json;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private WeatherData FetchWeatherData(String fileName) {
        WeatherData weatherData = new WeatherData();
        try {
            File myObj = new File(fileName);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] split = data.split(":");
                String key = split[0].strip();
                String value = split[1].strip();
                weatherData.setProperty(key, value);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return weatherData;
    }

    public static void main(String[] args) {
        //todo: retry when error
//        String url = args[0];
//        String filePath = args[1];
        ContentServer server = new ContentServer();
    }
    class UpdateWeatherTask extends TimerTask {
        private DataOutputStream outStream;
        private final LamportClock clock;
        public UpdateWeatherTask(DataOutputStream out, LamportClock clock) {
            outStream = out;
            this.clock = clock;
        }

        public void run() {
            String fileName = "SampleData";
            WeatherData data = FetchWeatherData(fileName);
            UpdateWeatherRequest request = new UpdateWeatherRequest(data,clock );
            String body = ConvertObjectToJson(request);
            String rq = CreatePutRequest(body);
            try {
                outStream.writeBytes(rq+"\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String CreatePutRequest(String body) {
        String method = "PUT";
        String resource = "/weather.json";
        String httpVersion = "HTTP/1.1";
        String userAgent = "ATOMClient/1/0";
        String contentType = "application/json";
        int contentLength = body.getBytes().length;

        StringBuilder request = new StringBuilder();
        request.append(method)
                .append(" ")
                .append(resource)
                .append(" ")
                .append(httpVersion)
                .append("\r\n");

        request.append("User-Agent: ")
                .append(userAgent)
                .append("\r\n");

        request.append("Content-Type: ")
                .append(contentType)
                .append("\r\n");

        request.append("Content-Length: ")
                .append(contentLength)
                .append("\r\n");

        request.append("\r\n");
        request.append(body);

        return request.toString();
    }
    class DisplayResponseThread extends Thread {
        private final LamportClock clock;
        private BufferedReader inStream;
        public DisplayResponseThread(BufferedReader brinp, LamportClock clock) {
            inStream = brinp;
            this.clock = clock;
        }
        public void run() {
            String message;
            StringBuilder gatheredMessage =  new StringBuilder();
            while (true) {
                try {
                    message = inStream.readLine();
                    if ((message == null) || message.equalsIgnoreCase("done")) {
                        socket.close();
                        return;
                    } else {
                        gatheredMessage.append(message).append("\n");
                        if (message.startsWith(String.valueOf('{')) && message.endsWith("}")) {
                            System.out.println(gatheredMessage.toString());
                            String body = message;
                            LamportClock responseClock = ConvertResponseToClockObj(body);
                            if (clock.counter<responseClock.counter) {
                                System.out.println("updating clock counter, become: " +responseClock.counter);
                                clock.UpdateTo(responseClock.counter);
                            }
                            gatheredMessage.delete(0, gatheredMessage.length());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        private LamportClock ConvertResponseToClockObj(String body) throws JsonProcessingException {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(body, LamportClock.class);
        }
    }
}
