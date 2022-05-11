package bitTorrent;

import be.adaxisoft.bencode.BEncodedValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static bitTorrent.BuildIHave.buildIHave;
import static bitTorrent.ConnectToPeer.connectToPeers;
import static bitTorrent.ParseTorrentFile.parseTorrentFile;
import static utility.GetHashValue.getHashValue;

public class BitTorrent {

    private static final Logger LOGGER = LogManager.getLogger(BitTorrent.class.getName());
    private  final List<byte[]> eachPiece = new ArrayList<>();
    private  final Map<String, BitSet> peersHave = new ConcurrentHashMap<>();
    private  final Map<String, Socket> peersSocket = new ConcurrentHashMap<>();
    private  final Map<Integer, byte[]> pieceReceived = new ConcurrentHashMap<>();
    private  final String peerId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    private  final boolean running = true;
    private final BitSet iHave;
    private final Map<String, BEncodedValue> info;


    public BitTorrent(int port) throws IOException {

        List<String> announces = new ArrayList<>();
        info = parseTorrentFile(eachPiece, announces);
        iHave = new BitSet(eachPiece.size());
        RandomAccessFile file = new RandomAccessFile(Path.of("target", info.get("name").getString()).toFile(), "rws");
        buildIHave(info, eachPiece, iHave);
        startService(port);
        connectToPeers(peersSocket, pieceReceived, peersHave, info, eachPiece, announces);
        sendRequest(iHave);
        doChecksum(iHave, file, info);
    }



    public static void main(String[] args) {

        try {
            new BitTorrent(Integer.parseInt(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private void doChecksum(BitSet iHave,RandomAccessFile file, Map<String, BEncodedValue> info) {
        Timer timer1 = new Timer("Timer");
        TimerTask task1 = new TimerTask() {
            public void run() {
                LOGGER.info("pieceReceived's size = " + pieceReceived.size());
                for (Map.Entry<Integer, byte[]> entry : pieceReceived.entrySet()) {
                    byte[] hash;
                    MessageDigest md;
                    try {
                        md = MessageDigest.getInstance("SHA-1");
                        hash = md.digest(entry.getValue());
                        if (Arrays.equals(hash, eachPiece.get(entry.getKey()))) {

                            iHave.set(entry.getKey());
                            LOGGER.info("iHave.cardinality() = " + iHave.cardinality());
                            if (iHave.cardinality() == eachPiece.size()) {
                                LOGGER.info("Check sum timer cancel.");
                                System.exit(0);
                                timer1.cancel();

                            }
                            file.seek((long) entry.getKey() * info.get("piece length").getInt());
                            file.write(entry.getValue());
                            pieceReceived.remove(entry.getKey());
                        }
                    } catch (NoSuchAlgorithmException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        timer1.schedule(task1, 0, 1000);
    }

    private void sendRequest(BitSet iHave) {
        ExecutorService executorServiceDoRequest = Executors.newFixedThreadPool(30);
        //  do request
        Timer timer = new Timer("Timer");
        TimerTask task = new TimerTask() {
            public void run() {
                for (Map.Entry<String, BitSet> bitSetEntry : peersHave.entrySet()) {
                    //LOGGER.info("peerhave = " + peersHave.size());
                    if (iHave.cardinality() == eachPiece.size()) {
                        timer.cancel();
                    }
                    executorServiceDoRequest.submit(() -> {
                        try {
                            DataOutputStream request = new DataOutputStream(peersSocket.get(bitSetEntry.getKey()).getOutputStream());
                            for (int i = 0; i < eachPiece.size(); i++) {  //i -> piece
                                //if (bitSetEntry.getValue().get(i) && !iHave.get(i)) {
                                if (!iHave.get(i)) {
                                    int pieceLen = 0;
                                    if (i != eachPiece.size() - 1) {
                                        pieceLen = info.get("piece length").getInt();
                                    } else {
                                        pieceLen = (int) (info.get("length").getLong() - (eachPiece.size() - 1) * info.get("piece length").getInt());
                                    }
                                    int begin = 0;
                                    //request: <len=0013><id=6><index><begin><length>
                                    while (pieceLen > 0) {
                                        request.writeInt(13);
                                        request.write(6);
                                        request.writeInt(i);
                                        request.writeInt(begin);
                                        request.writeInt(Math.min(pieceLen, 16384));
                                        request.flush();
                                        begin += Math.min(pieceLen, 16384);
                                        pieceLen -= Math.min(pieceLen, 16384);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            peersHave.remove(bitSetEntry.getKey());
                            peersSocket.remove(bitSetEntry.getKey());
                            LOGGER.info("Broken pipe");
                        }
                   });
                }

            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);
    }

    private void startService(int port) {
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
                    HandleRequest.handleRequest(iHave, hashValue, socket, info, peerId, eachPiece);
                });
            }
        }).start();
    }
}
