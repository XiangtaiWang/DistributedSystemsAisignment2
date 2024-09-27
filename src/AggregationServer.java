import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class AggregationServer{
    private final int port = 4567;
    private Socket socket   = null;
    private ServerSocket server   = null;
    public static void main(String[] args)  {
        AggregationServer aggregationServer = new AggregationServer();
    }
    public AggregationServer()
    {
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");
            System.out.println("Waiting for a client ...");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {

            try {
                socket = server.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            new SocketThread(socket).start();
        }

    }

    class SocketThread extends Thread{
        private Socket socket;
        private final RequestHandler requestHandler;
        public SocketThread(Socket clientSocket) {
            this.socket = clientSocket;
            requestHandler = new RequestHandler();
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
                        // todo: handle request
//                        System.out.println("received data");
//                        System.out.println(message);
                        gatheredMessage.append(message).append("\n");
                        if (message.startsWith(String.valueOf('{')) && message.endsWith("}")) {
//                            System.out.println("gatherd message before cleaning\n"+gatheredMessage);
                            String response = requestHandler.HandleRequest(gatheredMessage.toString());;
                            out.writeBytes(response + "\n\r");
                            out.flush();
                            gatheredMessage.delete(0, gatheredMessage.length());
//                            System.out.println("gatherd message after cleaning\n"+gatheredMessage);
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
    static class RequestHandler{
        private final String HistoryFile = "history.json";
        public RequestHandler() {

        }
        public String HandleRequest(String request) {
            System.out.println(request);
            String[] split = request.split("\n");

            String response;
            if (split[0].startsWith("PUT") ){
                String body = split[split.length - 1];
//                System.out.println(body);
                response = UpdateWeatherRequest(body);
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
            return null;
        }

        private String UpdateWeatherRequest(String body){
            //todo: 201 if first create, 200 update
            //todo: convert to object

            if (!HistoryFileHandler.IsFileExist()){
                HistoryFileHandler.CreateHistoryFile();
//                HistoryFileHandler.Update(body);
            }else{
//                HistoryFileHandler.Update(body);
            }
            return null;
        }
        private String GetWeatherRequest() {
            String data = HistoryFileHandler.GetWeather();
            // todo: create response (status 200)
            return null;
        }

        static class HistoryFileHandler{
            public static String fileName = "history.json";
            private ArrayList<WeatherData> records;
            public static boolean IsFileExist() {
                File f = new File(fileName);
                return f.exists() && !f.isDirectory();
            }

            public static void CreateHistoryFile() {
                File file = new File(fileName);
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public static void Update(WeatherData data) {
                // todo: if exist update, else insert

            }
            public static void Delete() {
                //todo: delete over 30s no update, where to call it?
                String data = GetWeather();
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    var map = mapper.readValue(data.toString(), WeatherData[].class);

                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            public static String GetWeather() {
                StringBuilder data = new StringBuilder();
                try {
                    File myObj = new File(fileName);
                    Scanner myReader = new Scanner(myObj);
                    while (myReader.hasNextLine()) {
                        data.append(myReader.nextLine());
                    }
                    myReader.close();
                } catch (FileNotFoundException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                return data.toString();

            }
        }
    }
}
