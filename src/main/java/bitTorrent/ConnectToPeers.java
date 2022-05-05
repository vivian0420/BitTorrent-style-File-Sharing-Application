package bitTorrent;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static bitTorrent.TalkToTracker.getTrackerResponse;

public class ConnectToPeers {

    private static final Logger LOGGER = LogManager.getLogger(ConnectToPeers.class.getName());
    private static List<Socket> sockets = new ArrayList<>();

    public static void connectToPeers(Map<String, BEncodedValue> info, Long length, List<String> announces) {

        /* How to calculate ip and port: https://stackoverflow.com/questions/50094674/how-to-parse-ip-and-port-from-http-tracker-response*/
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] body = getTrackerResponse(info, length, announces);

        try {
            Map<String, BEncodedValue> bodyMap = new BDecoder(new ByteArrayInputStream(body)).decodeMap().getMap();
            byte[] peers = bodyMap.get("peers").getBytes();
            outputStream.write(new byte[]{127, 0, 0, 1, 0x1a, (byte) 0xe1});
            outputStream.write(peers);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
            int counter = 0;
            while (in.available() > 0) {
                counter++;
                InetAddress address = InetAddress.getByAddress(new byte[]{in.readByte(), in.readByte(), in.readByte(), in.readByte()});
                int port1 = in.read();
                int port2 = in.read();
                int port = (port1 << 8) + port2;

                new Thread(()->{
                    try {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(address, port), 3000);
                        sockets.add(socket);
                        LOGGER.info("Connect succeed");
                    } catch (IOException e) {
                        LOGGER.info("Connect failed");
                    }
                }).start();

            }
            LOGGER.info("The number of peers: " + counter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
