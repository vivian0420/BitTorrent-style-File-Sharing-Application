package bitTorrent;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static bitTorrent.TalkToTracker.getTrackerResponse;

public class ConnectToPeers {

    private static final Logger LOGGER = LogManager.getLogger(ConnectToPeers.class.getName());
    private static final List<Socket> sockets = new ArrayList<>();
    private static final Map<String, BitSet> peersHave = new HashMap<>();
    private static final Map<Integer, byte[]> pieceReceived = new HashMap<>();
    private static final Map<String, Socket> peersSocket = new HashMap<>();

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
                new Thread(() -> {
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



            ExecutorService executorService = Executors.newFixedThreadPool(20);
            for (Socket socket : sockets) {
                executorService.execute(() -> {
                    // send handshake to peers
                    DataOutputStream out = null;
                    try {
                        out = new DataOutputStream(socket.getOutputStream());
                        out.write(19);
                        out.write("BitTorrent protocol".getBytes());
                        out.write(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
                        out.write(TalkToTracker.getInfoHash(info).getBytes());
                        out.write(TalkToTracker.peerId.getBytes());
                        out.flush();

                        //receive handshake form peers
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        if (input.read() != 19) {
                            socket.close();
                        }
                        if (!Arrays.equals(input.readNBytes("BitTorrent protocol".length()), "BitTorrent protocol".getBytes())) {
                            socket.close();
                        }
                        input.readNBytes(8);
                        if (!Arrays.equals(input.readNBytes(20), TalkToTracker.getInfoHash(info).getBytes())) {
                            return;
                        }
                        String peerId = new String(input.readNBytes(20));
                        peersSocket.put(peerId, socket);
                        BitSet bitSet = new BitSet();
                        peersHave.put(peerId, bitSet);

                        // handle peer's message
                        while (true) {
                            int len = input.readInt();
                            if (len == 0) {
                                continue;
                            }
                            int id = input.read();

                            if (id == 4) { // have: <len=0005><id=4><piece index>
                                int pieceIndex = input.readInt();
                                peersHave.get(peerId).set(pieceIndex);
                            } else if (id == 5) { // bitfield: <len=0001+X><id=5><bitfield>
                                byte[] payLoad = input.readNBytes(len - 1);
                                peersHave.get(peerId).or(BitSet.valueOf(payLoad));
                            } else if (id == 6) { //request: <len=0013><id=6><index><begin><length>
                                int index = input.readInt();
                                int begin = input.readInt();
                                int requestLength = input.readInt();
                                //TODO:
                            } else if (id == 7) { // piece: <len=0009+X><id=7><index><begin><block>
                                int index = input.readInt();
                                int begin = input.readInt();
                                byte[] block = input.readNBytes(len - 9);
                                if (!pieceReceived.containsKey(index)) {
                                    if (index != ParseTorrentFile.eachPiece.size() - 1) {
                                        pieceReceived.put(index, new byte[info.get("piece length").getInt()]);
                                    } else {
                                        int lastPieceLength = (int) (info.get("length").getLong() - (ParseTorrentFile.eachPiece.size() - 1) * info.get("piece length").getInt());
                                        pieceReceived.put(index, new byte[lastPieceLength]);
                                    }
                                }
                                //System.arraycopy(source_arr, sourcePos, dest_arr,destPos, len);
                                System.arraycopy(block, 0, pieceReceived.get(index), begin, block.length);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            LOGGER.info("The number of peers: " + counter);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
