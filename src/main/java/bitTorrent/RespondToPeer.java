package bitTorrent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static bitTorrent.ParseTorrentFile.info;

public class RespondToPeer {

    private static final Logger LOGGER = LogManager.getLogger(RespondToPeer.class.getName());
    private final boolean running = true;

    public RespondToPeer(int port) {
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            LOGGER.error("IOException:", e);
            return;
        }

        new Thread(()->{
            while(running) {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    LOGGER.error("IOException:", e);
                    continue;
                }
                executorService.execute(() -> {

                });
            }
        }).start();
    }

    public static void handleRequest(Socket socket) {
        try {
            DataInputStream in  = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());





        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
