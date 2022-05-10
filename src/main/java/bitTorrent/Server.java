package bitTorrent;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final Logger LOGGER = LogManager.getLogger(Server.class.getName());
    private static Map<String, BEncodedValue> info;
    private static final boolean running = true;

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        //Parse torrent file
        File torrentFile = Path.of("/Users/vivianzhang/dsd-final-project-vivian0420/test.torrent").toFile();
        FileInputStream inputStream = null;
        inputStream = new FileInputStream(torrentFile);
        BDecoder reader = new BDecoder(inputStream);
        Map<String, BEncodedValue> document = reader.decodeMap().getMap();
        info = document.get("info").getMap();
        int pieceLength = info.get("piece length").getInt();
        long fileLength = info.get("length").getLong();
        int pieceAmount;
        if((int)fileLength % pieceLength == 0) {
            pieceAmount = (int) (fileLength / pieceLength);
        } else {
            pieceAmount = (int) (fileLength / pieceLength) + 1;
        }
        LOGGER.info("piece amount = " + pieceAmount);



        //get info hash value
        MessageDigest hash;
        String infoHash;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BEncoder.encode(info, outputStream);
        hash = MessageDigest.getInstance("SHA-1");
        byte[] hashValue = hash.digest(outputStream.toByteArray());

        String serverPeerId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        // establish serverSocket
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
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
                    try {
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                        LOGGER.info("Checking handshake.");
                        //check handshake
                        if (in.read() != 19) {
                            socket.close();
                        }
                        if (!Arrays.equals(in.readNBytes("BitTorrent protocol".length()), "BitTorrent protocol".getBytes())) {
                            socket.close();
                        }
                        in.readNBytes(8);
                        if (!Arrays.equals(in.readNBytes(20), hashValue)) {
                            return;
                        }
                        in.readNBytes(20);
                        LOGGER.info("Received handshake from client.");

                        //reply handshake
                        out.write(19);
                        out.write("BitTorrent protocol".getBytes());
                        out.write(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
                        out.write(hashValue);
                        out.write(serverPeerId.getBytes());
                        out.flush();
                        LOGGER.info("Reply handshake to client.");

                        // sending bitfield

                        BitSet bitSet = new BitSet(pieceAmount);
                        bitSet.set(0, pieceAmount);
                        out.writeInt(1 + bitSet.toByteArray().length);
                        out.write(5);
                        out.write(bitSet.toByteArray());
                        out.flush();
                        LOGGER.info("Send bitfield to client.");


                        while(true) {

                            int len = in.readInt();
                            if (len == 0) { //keep-alive
                                continue;
                            }
                            int id = in.read();

                            if(id == 6) {
                                LOGGER.info("Received request.");
                                String name = info.get("name").getString();
                                int index = in.readInt();
                                LOGGER.info("Index = " + index);
                                int begin = in.readInt();
                                LOGGER.info("Begin = " + begin);
                                int length = in.readInt();

                                try(FileInputStream inStream = new FileInputStream(Path.of("/Users/vivianzhang/(COMIC1☆20) [23.4ド (イチリ)] ボクの理想の異世界生活10 (オリジナル) [DL版].zip").toFile())) {
                                    inStream.skipNBytes((long) (index * pieceLength) + begin);
                                    byte[] block = inStream.readNBytes(length);

                                    //piece: <len=0009+X><id=7><index><begin><block>
                                    out.writeInt(9 + length);
                                    out.write(7);
                                    out.writeInt(index);
                                    out.writeInt(begin);
                                    out.write(block);
                                    out.flush();
                                    LOGGER.info("Send piece.");
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }).start();
    }
}
