import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class AggregationServer{
    private Socket socket = null;
    private ServerSocket server = null;

    public static void main(String[] args)  {
        int port = 4567;
        try {
            port = Integer.parseInt(args[0]);
        } finally {
            AggregationServer aggregationServer = new AggregationServer(port);
        }
    }
    public AggregationServer(int port)
    {
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

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    class RemoveTooOldDataTask extends TimerTask {
        private final HistoryFileHandler historyFileHandler;
        public RemoveTooOldDataTask(HistoryFileHandler historyFileHandlerDI) {
            historyFileHandler = historyFileHandlerDI;
        }
        public void run() {
            historyFileHandler.CleaningHistoryData();
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
                        if (isBody(message)) {
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

        private boolean isBody(String message) {
            return message.startsWith(String.valueOf('{')) && message.endsWith("}");
        }
    }
}

