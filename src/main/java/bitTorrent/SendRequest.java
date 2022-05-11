package bitTorrent;

import be.adaxisoft.bencode.BEncodedValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
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

public class SendRequest {

    private static final Logger LOGGER = LogManager.getLogger(SendRequest.class.getName());


    public static void sendRequest(BitSet iHave, Map<String, BitSet> peersHave, Map<String, Socket> peersSocket,
                                   Map<String, BEncodedValue> info, List<byte[]> eachPiece, String mode) {
        ExecutorService executorServiceDoRequest = Executors.newFixedThreadPool(30);
        //  do request
        Timer timer = new Timer("Timer");
        TimerTask task = new TimerTask() {

            public void run() {
                if (BitTorrent.timerCancel) {
                    timer.cancel();
                }
                LOGGER.info("peersHave = " + peersHave.size());
                for (Map.Entry<String, BitSet> bitSetEntry : peersHave.entrySet()) {
                    Future<?> submit = executorServiceDoRequest.submit(() -> {
                        try {
                            DataOutputStream request = new DataOutputStream(peersSocket.get(bitSetEntry.getKey()).getOutputStream());
                            List<Integer> indexList = new ArrayList<>();
                            for (int i = 0; i < eachPiece.size(); i++) {  //i -> piece
                                if (!iHave.get(i)) {
                                    indexList.add(i);
                                }
                            }
                            Collections.shuffle(indexList);   //shuffle index list

                            for (int i : indexList) {
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
                            LOGGER.info("Sending request to peers.");
                        } catch (IOException e) {
                            peersHave.remove(bitSetEntry.getKey());
                            peersSocket.remove(bitSetEntry.getKey());
                            LOGGER.info("Broken pipe");
                        }
                    });

                    try {
                        submit.get(10000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        submit.cancel(true);                 // Give up this thread if haven't received any data in 6s
                        //peersHave.remove(bitSetEntry.getKey());
                        //peersSocket.remove(bitSetEntry.getKey());
                    }

                }
            }
        };
        timer.scheduleAtFixedRate(task, 2000, 1000);
    }
}
