package bitTorrent;

import be.adaxisoft.bencode.BEncodedValue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static bitTorrent.ConnectToPeer.connectToPeers;
import static bitTorrent.DoChecksum.doChecksum;
import static bitTorrent.ParseTorrentFile.parseTorrentFile;
import static bitTorrent.SendRequest.sendRequest;
import static bitTorrent.StartServerSocket.startService;
import static utility.BuildIHave.buildIHave;

public class BitTorrent {

    public static boolean timerCancel = false;
    private final List<String> announces = new ArrayList<>();
    private final List<byte[]> eachPiece = new ArrayList<>();
    private final Map<byte[], Socket> clientSockets = new ConcurrentHashMap<>();
    private final Map<Integer, byte[]> pieceReceived = new ConcurrentHashMap<>();
    private final Map<String, BitSet> peersHave = new ConcurrentHashMap<>();
    private final Map<String, Socket> peersSocket = new ConcurrentHashMap<>();
    private final BitSet iHave;
    private final RandomAccessFile file;
    private final Map<String, BEncodedValue> info;
    private final String mode;
    private final String peerHostName;
    private final int peerPort;


    public BitTorrent(int port, String torrentFilePath, String mode, String peerHostName, int peerPort) throws IOException {
        this.mode = mode;
        this.peerHostName = peerHostName;
        this.peerPort = peerPort;
        info = parseTorrentFile(eachPiece, announces, torrentFilePath);
        iHave = new BitSet(eachPiece.size());
        Files.createDirectories(Path.of("target", String.valueOf(port)));
        file = new RandomAccessFile(Path.of("target", String.valueOf(port), info.get("name").getString()).toFile(), "rws");
        buildIHave(info, eachPiece, iHave, port);
        startService(port, info, iHave, clientSockets);
    }

    public void download () {
        connectToPeers(peersSocket, pieceReceived, peersHave, info, eachPiece, announces, mode, peerHostName, peerPort);
        sendRequest(iHave, peersHave, peersSocket, info, eachPiece);
        doChecksum(iHave, file, info, pieceReceived, eachPiece, clientSockets);
    }



    public static void main(String[] args) {
        try {
            if (args.length > 3) {
                new BitTorrent(Integer.parseInt(args[0]), args[1], args[2], args[3], Integer.parseInt(args[4])).download();
            } else {
                new BitTorrent(Integer.parseInt(args[0]), args[1], args[2], null, -1).download();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}
