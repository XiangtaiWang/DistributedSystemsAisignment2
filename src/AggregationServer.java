import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class AggregationServer{
    private final int port = 4567;
    private Socket socket   = null;
    private ServerSocket server   = null;
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

            // new thread for a client
            new SocketThread(socket).start();
        }

    }
    public static void main(String[] args)  {
        AggregationServer aggregationServer = new AggregationServer();
    }

    class SocketThread extends Thread{
        private Socket socket;
        public SocketThread(Socket clientSocket) {
            this.socket = clientSocket;
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
            String line;
            while (true) {
                try {
                    line = brinp.readLine();
                    if ((line == null) || line.equalsIgnoreCase("done")) {
                        socket.close();
                        return;
                    } else {
                        System.out.println(line);
                        out.writeBytes(line + "\n\r");
                        out.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
