package bitTorrent;

import be.adaxisoft.bencode.BEncodedValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.BitSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static utility.GetHashValue.getHashValue;

public class StartServerSocket {

    private static final Logger LOGGER = LogManager.getLogger(StartServerSocket.class.getName());
    private static final String myId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    private static final boolean running = true;

    /* Start server socket to handle peers' requests */
    public static  void startService(int port, Map<String, BEncodedValue> info, BitSet iHave, Map<byte[], Socket> clientSockets) {
        byte[] hashValue = getHashValue(info);
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
                    LOGGER.info("Get connected.");
                } catch (IOException e) {
                    LOGGER.error("IOException:", e);
                    continue;
                }
                executorService.execute(() -> {
                    HandleRequest.handleRequest(iHave, hashValue, socket, info, myId, port, clientSockets);
                });
            }
        }).start();
    }
}
