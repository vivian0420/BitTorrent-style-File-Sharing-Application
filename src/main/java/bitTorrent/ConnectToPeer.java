package bitTorrent;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.InvalidBEncodingException;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static bitTorrent.ConnectToTracker.connectToTracker;
import static utility.GetUriList.getUriList;
import static utility.GetHashValue.getHashValue;

public class ConnectToPeer {

    private static final Logger LOGGER = LogManager.getLogger(ConnectToPeer.class.getName());

    public static void connectToPeers(Map<String, Socket> peersSocket, Map<Integer, byte[]> pieceReceived, Map<String, BitSet> peersHave,
                                      Map<String, BEncodedValue> info, List<byte[]> eachPiece, List<String> announces, String mode,
                                      String hostName, int port) {


        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Timer connectToPeers = new Timer("connectToPeers");
        connectToPeers.schedule(new TimerTask() {
            @Override
            public void run() {
                String myId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
                List<URI> uriList;
                try {
                    uriList = getUriList(info, myId, announces);
                } catch (InvalidBEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                List<byte[]> bodies = connectToTracker(uriList);
                List<InetSocketAddress> addresses = getInetSocketAddress(bodies, mode);
                if (mode.equals("local")) {
                    addresses.add(new InetSocketAddress(hostName, port));
                }
                for (InetSocketAddress address : addresses) {
                    executorService.execute(() -> {
                        try {
                            LOGGER.info("Try to Connect peer");
                            Socket socket = new Socket();
                            socket.connect(address, 5000);
                            //LOGGER.info("Connect succeed");

                            DataInputStream peerMessage = new DataInputStream(socket.getInputStream());

                            //do handshake
                            doHandshake(socket, info, myId);

                            //receive handshake form peers
                            String peerId = receiveHandshake(socket, peerMessage, info, peersSocket, peersHave);
                            if (peerId == null) return;

                            // handle peer's message: have, bitfield, piece
                            handlePeersMessage(peerMessage, peerId, peersHave, pieceReceived, eachPiece, info);
                        } catch (IOException e) {
                            LOGGER.info("Connect failed");
                        }
                    });
                }
            }
        }, 0, 60000);
    }


    private static String receiveHandshake(Socket socket, DataInputStream peerMessage, Map<String, BEncodedValue> info, Map<String, Socket> peersSocket, Map<String, BitSet> peersHave) throws IOException {
        if (peerMessage.read() != 19) {
            socket.close();
        }
        if (!Arrays.equals(peerMessage.readNBytes("BitTorrent protocol".length()), "BitTorrent protocol".getBytes())) {
            socket.close();
        }
        peerMessage.readNBytes(8);
        if (!Arrays.equals(peerMessage.readNBytes(20), getHashValue(info))) {
            return null;
        }
        String peerId = new String(peerMessage.readNBytes(20));
        peersSocket.put(peerId, socket);
        BitSet bitSet = new BitSet();
        peersHave.put(peerId, bitSet);
        LOGGER.info("Received handshake from peer.");
        return peerId;
    }

    private static void handlePeersMessage(DataInputStream peerMessage, String peerId, Map<String, BitSet> peersHave, Map<Integer, byte[]> pieceReceived, List<byte[]> eachPiece, Map<String, BEncodedValue> info) throws IOException {
        while (true) {
            int len = peerMessage.readInt();
            if (len == 0) { //keep-alive
                continue;
            }
            if (len < 0) {
                break;
            }
            int id = peerMessage.read();

            if (id == 4) {    // have: <len=0005><id=4><piece index>
                int pieceIndex = peerMessage.readInt();
                peersHave.get(peerId).set(pieceIndex);
            } else if (id == 5) {    // bitfield: <len=0001+X><id=5><bitfield>
                LOGGER.info("Received bitfield from peer.");
                byte[] payLoad = peerMessage.readNBytes(len - 1);
                peersHave.get(peerId).or(BitSet.valueOf(payLoad));
            } else if (id == 7) {    // piece: <len=0009+X><id=7><index><begin><block>
                int index = peerMessage.readInt();
                int begin = peerMessage.readInt();
                byte[] block = peerMessage.readNBytes(len - 9);
                if (!pieceReceived.containsKey(index)) {    // if the piece isn't the last piece
                    if (index != eachPiece.size() - 1) {
                        pieceReceived.put(index, new byte[info.get("piece length").getInt()]);
                    } else {    // if the piece is the last piece
                        int lastPieceLength = (int) (info.get("length").getLong() - (eachPiece.size() - 1) * info.get("piece length").getInt());
                        pieceReceived.put(index, new byte[lastPieceLength]);
                    }
                }
                /* System.arraycopy(source_arr, sourcePos, dest_arr,destPos, len); */
                System.arraycopy(block, 0, pieceReceived.get(index), begin, block.length);
            } else if (id == 6) {   //request: <len=0013><id=6><index><begin><length>
                peerMessage.readInt();
                peerMessage.readInt();
                peerMessage.readInt();
            } else if (id == 8) {   //cancel: <len=0013><id=8><index><begin><length>
                peerMessage.readInt();
                peerMessage.readInt();
                peerMessage.readInt();
            } else if (id == 9) {   //port: <len=0003><id=9><listen-port>
                peerMessage.readInt();
            }
        }
    }

    private static void doHandshake(Socket socket, Map<String, BEncodedValue> info, String myId) throws IOException {
        DataOutputStream writeHandshake = new DataOutputStream(socket.getOutputStream());
        writeHandshake.write(19);
        writeHandshake.write("BitTorrent protocol".getBytes());
        writeHandshake.write(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
        writeHandshake.write(getHashValue(info));
        writeHandshake.write(myId.getBytes());
        writeHandshake.flush();
        LOGGER.info("Sending handshake...");
    }


    private static List<InetSocketAddress> getInetSocketAddress(List<byte[]> bodies, String mode) {
        List<InetSocketAddress> addresses = new ArrayList<>();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] body : bodies) {
                Map<String, BEncodedValue> bodyMap = new BDecoder(new ByteArrayInputStream(body)).decodeMap().getMap();
                byte[] peers = bodyMap.get("peers").getBytes();
                if (mode.equals("online")) {
                    out.write(peers);
                }
            }
            //out.write(new byte[]{127, 0, 0, 1, 0x1a, (byte) 0xe1});
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));

            while (in.available() > 0) {
                InetAddress address = InetAddress.getByAddress(new byte[]{in.readByte(), in.readByte(), in.readByte(), in.readByte()});
                int port1 = in.read();
                int port2 = in.read();
                int peerPort = (port1 << 8) + port2;
                addresses.add(new InetSocketAddress(address, peerPort));
            }

        } catch (IOException e) {
            LOGGER.info("failed to get peers from tracker response");
        }
        return addresses;
    }

}
