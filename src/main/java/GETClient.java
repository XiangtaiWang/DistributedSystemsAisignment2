import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class GETClient {
    private BufferedReader brinp;
    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;
    public GETClient(String address, int port, String id){
        //initialize socket connection, retry every 5s
        //get weather data every 5s
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

        Timer timer = new Timer();
        timer.schedule(new GetWeather(out, id), 0, 5000);
        new DisplayResponseThread(brinp).start();

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
    public static void main(String[] args) {
        String url = args[0];
        String[] split = url.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        String id  = "all";
        try {
            id = args[1];
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        GETClient server = new GETClient(host, port, id);
    }
    class GetWeather extends TimerTask {
        private DataOutputStream outStream;
        private String id;
        public GetWeather(DataOutputStream out, String id) {
            outStream = out;
            this.id = id;
        }

        public void run() {
            // send get request
            String rq = CreateGetRequest(id, "{}");
            try {
                outStream.writeBytes(rq+"\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String CreateGetRequest(String id, String body) {
        // generate http get request
        String method = "GET";
        String resource = id;
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
        private BufferedReader inStream;
        private LamportClock clock = new LamportClock();
        public DisplayResponseThread(BufferedReader brinp) {
            inStream = brinp;
        }
        public void run() {
            // accept response and update lamport clock
            String message;
            StringBuilder gatheredMessage =  new StringBuilder();
            while (true) {
                try {
                    message = inStream.readLine();
                    gatheredMessage.append(message).append("\n");
                    if (message.startsWith(String.valueOf('{')) && message.endsWith("}")) {
                        System.out.println(gatheredMessage.toString());
                        HistoryContent historyContent = ConvertToHistoryContentObj(message);
                        clock.UpdateTo(historyContent.clock.counter);
                        System.out.println("updating my clock to=> " + historyContent.clock.counter);
                        gatheredMessage.delete(0, gatheredMessage.length());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }

        }
        private HistoryContent ConvertToHistoryContentObj(String data) {
            ObjectMapper objectMapper = AggregationServer.getObjectMapper();
            HistoryContent history;
            try {
                history = objectMapper.readValue(data, HistoryContent.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return history;
        }

    }
}
