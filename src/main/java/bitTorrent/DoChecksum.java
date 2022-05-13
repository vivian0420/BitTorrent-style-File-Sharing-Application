package bitTorrent;

import be.adaxisoft.bencode.BEncodedValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DoChecksum {

    private static final Logger LOGGER = LogManager.getLogger(DoChecksum.class.getName());

    public static void doChecksum(BitSet iHave, RandomAccessFile file, Map<String, BEncodedValue> info, Map<Integer,
            byte[]> pieceReceived, List<byte[]> eachPiece, Map<byte[], Socket> clientSockets) {
        ExecutorService executorServiceSendHave = Executors.newFixedThreadPool(20);
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
                                timer1.cancel();
                                BitTorrent.timerCancel = true;
                            }
                            file.seek((long) entry.getKey() * info.get("piece length").getInt());
                            file.write(entry.getValue());
                            //send "have" to peers.
                            Future<?> submit = executorServiceSendHave.submit(() -> {
                                for (Map.Entry<byte[], Socket> socketEntry : clientSockets.entrySet()) {  // have: <len=0005><id=4><piece index>
                                    synchronized (socketEntry.getValue()) {
                                        try {
                                            DataOutputStream sendHave = new DataOutputStream(socketEntry.getValue().getOutputStream());
                                            sendHave.writeInt(5);
                                            sendHave.write(4);
                                            sendHave.writeInt(entry.getKey());
                                            sendHave.flush();
                                            LOGGER.info("sending have to peers>>>>>>>>>>>>>>>>");
                                        } catch (IOException e) {
                                            LOGGER.info("broken pipe.");
                                        }
                                    }
                                }
                            });
                            try {
                                submit.get(3000, TimeUnit.MILLISECONDS);
                            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                                submit.cancel(true);
                            }
                            pieceReceived.remove(entry.getKey());  //Do not need to do checksum on this piece anymore.
                        }
                    } catch (NoSuchAlgorithmException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        timer1.schedule(task1, 2000, 1000);
    }

}
