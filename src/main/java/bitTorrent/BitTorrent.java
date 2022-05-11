package bitTorrent;

import be.adaxisoft.bencode.BEncodedValue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
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


    public BitTorrent(int port) throws IOException {

        List<String> announces = new ArrayList<>();
        List<byte[]> eachPiece = new ArrayList<>();
        Map<Integer, byte[]> pieceReceived = new ConcurrentHashMap<>();
        Map<String, BitSet> peersHave = new ConcurrentHashMap<>();
        Map<String, Socket> peersSocket = new ConcurrentHashMap<>();

        Map<String, BEncodedValue> info = parseTorrentFile(eachPiece, announces);
        BitSet iHave = new BitSet(eachPiece.size());
        RandomAccessFile file = new RandomAccessFile(Path.of("target", info.get("name").getString()).toFile(), "rws");
        buildIHave(info, eachPiece, iHave);
        startService(port, info, iHave, eachPiece);
        connectToPeers(peersSocket, pieceReceived, peersHave, info, eachPiece, announces);
        sendRequest(iHave, peersHave, peersSocket, info, eachPiece);
        doChecksum(iHave, file, info, pieceReceived, eachPiece);
    }



    public static void main(String[] args) {
        try {
            new BitTorrent(Integer.parseInt(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}
