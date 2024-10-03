import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class GETClient {
    private BufferedReader brinp;
    private String address = "127.0.0.1";
    private int port = 4567;
    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;
    public GETClient(){
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

        Timer timer = new Timer();
        timer.schedule(new GetWeather(out), 0, 5000);
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
        GETClient server = new GETClient();
    }
    class GetWeather extends TimerTask {
        private DataOutputStream outStream;
        public GetWeather(DataOutputStream out) {
            outStream = out;
        }

        public void run() {

            String rq = CreateGetRequest("{}");
            try {
                outStream.writeBytes(rq+"\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String CreateGetRequest(String body) {
        String method = "GET";
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
        private BufferedReader inStream;
        public DisplayResponseThread(BufferedReader brinp) {
            inStream = brinp;
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
}
