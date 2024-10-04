import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ContentServer{
    private BufferedReader brinp;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream out;

    public ContentServer(String address, int port, String fileName){
        //initialize socket connection, retry every 5s
        //update weather request every 5s
        while (true){
            try {
                socket = new Socket(address, port);
                System.out.println("Connected");
                InputStream inp = socket.getInputStream();
                brinp = new BufferedReader(new InputStreamReader(inp));
                input = new DataInputStream(System.in);
                out = new DataOutputStream(socket.getOutputStream());
                break;
            }
            catch (IOException i) {
                System.out.println(i);
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Retrying connect");
            }
        }

        LamportClock clock = new LamportClock();
        Timer timer = new Timer();
        UpdateWeatherTask task = new UpdateWeatherTask(out, clock, fileName);
        timer.schedule(task, 0, 5000);
        ResponseThread responseThread = new ResponseThread(brinp, clock, task);
        responseThread.start();

        ContinuouslyAcceptUserInput();
        try {
            task.cancel();
            CloseConnection();
            responseThread.interrupt();
        }
        catch (IOException ignored) {
        }
        finally {
            System.exit(0);
        }

    }

    private void ContinuouslyAcceptUserInput() {
        String line = "";
        while (!line.equals("done")) {
            try {
                line = input.readLine();
                out.writeBytes(line+"\n");
            }
            catch (IOException i) {
                System.out.println(i);
                break;
            }
        }
    }

    private void CloseConnection() throws IOException {
        input.close();
        out.close();
        socket.close();
        brinp.close();
    }

    private String ConvertObjectToJson(Object data) {
        // json serialize obj
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
        // read from file then convert to obj
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
            System.exit(1);
        }
        return weatherData;
    }

    public static void main(String[] args) {
        String url = args[0];
        String[] split = url.split(":");

        String address = split[0];
        int port = Integer.parseInt(split[1]);
        String filePath = args[1];

        ContentServer server = new ContentServer(address, port,filePath);
    }
    class UpdateWeatherTask extends TimerTask {
        private DataOutputStream outStream;
        private final LamportClock clock;
        private boolean running = true;
        private String fileName;

        public UpdateWeatherTask(DataOutputStream out, LamportClock clock, String fileName) {
            outStream = out;
            this.clock = clock;
            this.fileName = fileName;
        }

        public void run() {
            // Get Weather data from file, send request
            if (running){
                WeatherData data = FetchWeatherData(fileName);
                WeatherRequest request = new WeatherRequest(data,clock );
                String body = ConvertObjectToJson(request);
                String rq = CreatePutRequest(body);
                try {
                    outStream.writeBytes(rq+"\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    private String CreatePutRequest(String body) {
        //generate http request
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
    class ResponseThread extends Thread {
        private final LamportClock clock;
        private BufferedReader inStream;
        private UpdateWeatherTask updateWeatherTask;
        public ResponseThread(BufferedReader brinp, LamportClock clock, UpdateWeatherTask task) {
            inStream = brinp;
            this.clock = clock;
            updateWeatherTask = task;
        }
        public void run() {
            //accept response from server, update clock when full message received
            // when internal server error, wait few seconds then continue
            String message;
            StringBuilder gatheredMessage =  new StringBuilder();
            while (true) {
                try {
                    message = inStream.readLine();
                    gatheredMessage.append(message).append("\n");
                    if (isBody(message)) {
                        System.out.println(gatheredMessage.toString());
                        String response = gatheredMessage.toString();
                        if (response.contains(HttpStatus.HTTP_SUCCESS.name())) {
                            String body = message;
                            LamportClock responseClock = ConvertResponseToClockObj(body);
                            if (clock.counter<responseClock.counter) {
                                System.out.println("updating clock counter, become: " +responseClock.counter);
                                clock.UpdateTo(responseClock.counter);
                            }
                            gatheredMessage.delete(0, gatheredMessage.length());
                        } else if (response.contains(HttpStatus.HTTP_INTERNAL_ERROR.name())) {
                            updateWeatherTask.running = false;
                            System.out.println("internal Server error, take a break");
                            try {
                                TimeUnit.SECONDS.sleep(3);
                            } catch (InterruptedException ignored) {
                            }
                            updateWeatherTask.running = true;
                            System.out.println("enable update weather task again");
                        } else if (response.contains(HttpStatus.HTTP_BAD_REQUEST.name())) {
                            System.out.println("bad request");
                        }else
                        {
                            System.out.println("unexpected response!!!");
                        }
                    }
                } catch (IOException e) {
//                    e.printStackTrace();
                    return;
                }
            }
        }

        private boolean isBody(String message) {
            return message.startsWith(String.valueOf('{')) && message.endsWith("}");
        }

        private LamportClock ConvertResponseToClockObj(String body) throws JsonProcessingException {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(body, LamportClock.class);
        }
    }
}
