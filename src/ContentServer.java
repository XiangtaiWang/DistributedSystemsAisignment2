import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;


public class ContentServer{
    //todo: read response after put request
    //todo: make put request
    private String address = "127.0.0.1";
    private int port = 4567;
    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;

    public ContentServer(){
        try {
            var socket = new Socket(address, port);
            System.out.println("Connected");
            input = new DataInputStream(System.in);
            out = new DataOutputStream(
                    socket.getOutputStream());
        }
        catch (IOException i) {
            System.out.println(i);
            return;
        }


        Timer timer = new Timer();
        timer.schedule(new UpdateWeather(out), 0, 5000);

//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("http://foo.com/"))
//                .method("PUT", HttpRequest.BodyPublishers.ofString(dataString))
//                .build();
//        String rq = ConvertObjectToJson(request);

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
            System.out.println(json);
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
        //todo
//        String url = args[0];
//        String filePath = args[1];
        ContentServer server = new ContentServer();
    }
    class UpdateWeather extends TimerTask {
        private DataOutputStream outStream;
        public UpdateWeather(DataOutputStream out) {
            outStream = out;
        }

        public void run() {
            String fileName = "SampleData";
            WeatherData data = FetchWeatherData(fileName);
            String dataString = ConvertObjectToJson(data);
            try {
                outStream.writeBytes(dataString+"\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
