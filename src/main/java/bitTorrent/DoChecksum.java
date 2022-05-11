package bitTorrent;

import be.adaxisoft.bencode.BEncodedValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DoChecksum {

    private static final Logger LOGGER = LogManager.getLogger(DoChecksum.class.getName());
    public static void doChecksum(BitSet iHave, RandomAccessFile file, Map<String, BEncodedValue> info, Map<Integer, byte[]> pieceReceived, List<byte[]> eachPiece) {
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

}
