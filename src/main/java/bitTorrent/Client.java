package bitTorrent;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private static final Logger LOGGER = LogManager.getLogger(ParseTorrentFile.class.getName());
    private static final List<byte[]> eachPiece = new ArrayList<>();
    private static final Map<String, BitSet> peersHave = new HashMap<>();
    private static final Map<String, Socket> peersSocket = new HashMap<>();
    private static final Map<Integer, byte[]> pieceReceived = new HashMap<>();
    private static Map<String, BEncodedValue> info;


    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException {

        //Parse torrent file
        File torrentFile = Path.of("/Users/vivianzhang/dsd-final-project-vivian0420/test.torrent").toFile();
        FileInputStream inputStream = null;
        inputStream = new FileInputStream(torrentFile);
        BDecoder reader = new BDecoder(inputStream);
        Map<String, BEncodedValue> document = reader.decodeMap().getMap();
        String announce = document.get("announce").getString();
        List<BEncodedValue> announceList = document.get("announce-list").getList();

        List<String> announces = new ArrayList<>();
        if (announce.startsWith("http")) {
            announces.add(announce);
            LOGGER.info("Announce: " + announce);
        }
        for (BEncodedValue b : announceList) {
            String announceString = b.getList().get(0).getString();
            if (announceString.startsWith("http")) {
                announces.add(announceString);
                LOGGER.info("Announce: " + announceString);
            }
        }
        info = document.get("info").getMap();
        String name = info.get("name").getString();
        Long length = info.get("length").getLong();
        int pieceLength = info.get("piece length").getInt();
        byte[] pieces = info.get("pieces").getBytes();
        DataInputStream piecesIn = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(pieces)));
        while (piecesIn.available() > 0) {
            eachPiece.add(piecesIn.readNBytes(20));
        }




        //get InfoHash
        MessageDigest hash;
        String infoHash;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BEncoder.encode(info, outputStream);
        hash = MessageDigest.getInstance("SHA-1");
        byte[] hashValue = hash.digest(outputStream.toByteArray());
        infoHash = URLEncoder.encode(new String(hashValue, StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);


        //getURL
        String clientPeerId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String port = String.valueOf(6881);
        String left = String.valueOf(length);
        String downloaded = String.valueOf(0L);
        String uploaded = String.valueOf(0L);

        List<URI> uriList = new ArrayList<>();
        for (String eachAnnounce : announces) {
            URI uri = URI.create(eachAnnounce + "?" + "info_hash=" + infoHash + "&peer_id=" + clientPeerId + "&port=" + port + "&left=" + left + "&downloaded=" + downloaded + "&uploaded=" + uploaded + "&compact=1");
            uriList.add(uri);
        }


        //Talk To Tracker
        List<byte[]> bodies = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();
        for (URI uri : uriList) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .build();

            try {
                HttpResponse<byte[]> response =
                        client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                bodies.add(response.body());
            } catch (IOException e) {
                LOGGER.info("Failed to connect to this tracker.");
            }


        }


        //Connect to peer
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] body : bodies) {
            Map<String, BEncodedValue> bodyMap = new BDecoder(new ByteArrayInputStream(body)).decodeMap().getMap();
            byte[] peers = bodyMap.get("peers").getBytes();
            //out.write(peers);
        }
        out.write(new byte[]{127, 0, 0, 1, 0x1a, (byte) 0xe1});
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        while (in.available() > 0) {
            InetAddress address = InetAddress.getByAddress(new byte[]{in.readByte(), in.readByte(), in.readByte(), in.readByte()});
            int port1 = in.read();
            int port2 = in.read();
            int peerPort = (port1 << 8) + port2;
            executorService.execute(() -> {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(address, peerPort));
                    LOGGER.info("Connect succeed");


                    //do handshake
                    DataOutputStream writeHandshake = new DataOutputStream(socket.getOutputStream());
                    writeHandshake.write(19);
                    writeHandshake.write("BitTorrent protocol".getBytes());
                    writeHandshake.write(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
                    writeHandshake.write(hashValue);
                    writeHandshake.write(clientPeerId.getBytes());
                    writeHandshake.flush();


                    //receive handshake form peers
                    DataInputStream peerMessage = new DataInputStream(socket.getInputStream());
                    if (peerMessage.read() != 19) {
                        socket.close();
                    }
                    if (!Arrays.equals(peerMessage.readNBytes("BitTorrent protocol".length()), "BitTorrent protocol".getBytes())) {
                        socket.close();
                    }
                    peerMessage.readNBytes(8);
                    if (!Arrays.equals(peerMessage.readNBytes(20), hashValue)) {
                        return;
                    }
                    String peerId = new String(peerMessage.readNBytes(20));
                    peersSocket.put(peerId, socket);
                    BitSet bitSet = new BitSet();
                    peersHave.put(peerId, bitSet);


                    // handle peer's message: have, bitfield, piece
                    while (true) {
                        int len = peerMessage.readInt();
                        if (len == 0) { //keep-alive
                            continue;
                        }
                        int id = peerMessage.read();

                        if (id == 4) {    // have: <len=0005><id=4><piece index>
                            int pieceIndex = peerMessage.readInt();
                            peersHave.get(peerId).set(pieceIndex);
                        } else if (id == 5) {    // bitfield: <len=0001+X><id=5><bitfield>
                            byte[] payLoad = peerMessage.readNBytes(len - 1);
                            peersHave.get(peerId).or(BitSet.valueOf(payLoad));
                        } else if (id == 7) {    // piece: <len=0009+X><id=7><index><begin><block>
                            int index = peerMessage.readInt();
                            int begin = peerMessage.readInt();
                            byte[] block = peerMessage.readNBytes(len - 9);
                            if (!pieceReceived.containsKey(index)) {    // if the piece isn't the last piece
                                if (index != ParseTorrentFile.eachPiece.size() - 1) {
                                    pieceReceived.put(index, new byte[info.get("piece length").getInt()]);
                                } else {    // if the piece is the last piece
                                    int lastPieceLength = (int) (info.get("length").getLong() - (ParseTorrentFile.eachPiece.size() - 1) * info.get("piece length").getInt());
                                    pieceReceived.put(index, new byte[lastPieceLength]);
                                }
                            }
                            //System.arraycopy(source_arr, sourcePos, dest_arr,destPos, len);
                            System.arraycopy(block, 0, pieceReceived.get(index), begin, block.length);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.info("Connect failed");
                }
            });
        }


        final BitSet iHave = new BitSet(eachPiece.size());
        //  do request
        new Thread(() -> {
            TimerTask task = new TimerTask() {
                public void run() {
                    for (Map.Entry<String, BitSet> bitSetEntry : peersHave.entrySet()) {
                        try {
                            DataOutputStream request = new DataOutputStream(peersSocket.get(bitSetEntry.getKey()).getOutputStream());
                            for (int i = 0; i < bitSetEntry.getValue().size(); i++) {  //i -> piece
                                if (bitSetEntry.getValue().get(i) && !iHave.get(i)) {
                                    int pieceLen = 0;
                                    if (i != eachPiece.size() - 1) {
                                        pieceLen = info.get("piece length").getInt();
                                    } else {
                                        pieceLen = (int) (info.get("length").getLong() - (eachPiece.size() - 1) * info.get("piece length").getInt());
                                    }
                                    int begin = 0;
                                    while (pieceLen > 0) {
                                        request.write(13);
                                        request.write(6);
                                        request.write(i);
                                        request.write(begin);
                                        request.write(Math.min(pieceLen, 16384));
                                        request.flush();
                                        pieceLen -= Math.min(pieceLen, 16384);
                                        begin += Math.min(pieceLen, 16384);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            };
            Timer timer = new Timer("Timer");
            timer.schedule(task, 1000, 1000);
        }).start();





        RandomAccessFile file = new RandomAccessFile(Path.of("target", info.get("name").getString()).toFile(), "rws");
        // checkSum
        new Thread(() -> {
            TimerTask task = new TimerTask() {
                public void run() {
                    for (Map.Entry<Integer, byte[]> entry : pieceReceived.entrySet()) {
                        byte[] hash;
                        MessageDigest md;
                        try {
                            md = MessageDigest.getInstance("SHA-1");
                            hash = md.digest(entry.getValue());
                            if (Arrays.equals(hash, eachPiece.get(entry.getKey()))) {
                                for (Map.Entry<String, BitSet> peerHaveEntry: peersHave.entrySet()) {
                                    if (peerHaveEntry.getValue().get(entry.getKey())) {
                                        iHave.set(entry.getKey());
                                        file.seek((long) entry.getKey() * pieceLength);
                                        file.write(entry.getValue());
                                    }
                                }
                            }
                        } catch (NoSuchAlgorithmException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Timer timer = new Timer("Timer");
            timer.schedule(task, 1000, 1000);
        }).start();
    }
}
