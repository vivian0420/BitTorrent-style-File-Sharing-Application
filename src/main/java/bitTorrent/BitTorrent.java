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

    public BitTorrent(int port, String torrentFilePath, String mode, String peerHostName, int peerPort) throws IOException {

        List<String> announces = new ArrayList<>();
        List<byte[]> eachPiece = new ArrayList<>();
        Map<byte[], Socket> clientSockets = new ConcurrentHashMap<>();
        Map<Integer, byte[]> pieceReceived = new ConcurrentHashMap<>();
        Map<String, BitSet> peersHave = new ConcurrentHashMap<>();
        Map<String, Socket> peersSocket = new ConcurrentHashMap<>();

        Map<String, BEncodedValue> info = parseTorrentFile(eachPiece, announces, torrentFilePath);
        BitSet iHave = new BitSet(eachPiece.size());
        Files.createDirectories(Path.of("target", String.valueOf(port)));
        RandomAccessFile file = new RandomAccessFile(Path.of("target", String.valueOf(port), info.get("name").getString()).toFile(), "rws");
        buildIHave(info, eachPiece, iHave, port);
        startService(port, info, iHave, clientSockets);
        connectToPeers(peersSocket, pieceReceived, peersHave, info, eachPiece, announces, mode, peerHostName, peerPort);
        sendRequest(iHave, peersHave, peersSocket, info, eachPiece, mode);
        doChecksum(iHave, file, info, pieceReceived, eachPiece, clientSockets);
    }



    public static void main(String[] args) {
        try {
            if (args.length > 3) {
                new BitTorrent(Integer.parseInt(args[0]), args[1], args[2], args[3], Integer.parseInt(args[4]));
            } else {
                new BitTorrent(Integer.parseInt(args[0]), args[1], args[2], null, -1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}
