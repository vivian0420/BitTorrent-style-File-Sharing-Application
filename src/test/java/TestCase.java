import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import bitTorrent.BitTorrent;
import bitTorrent.ConnectToPeer;
import org.junit.jupiter.api.Test;
import utility.GetHashValue;
import utility.GetUriList;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static bitTorrent.ConnectToTracker.connectToTracker;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCase {

    @Test
    public void testParseTorrentFile() throws IOException {

        File torrentFile = Path.of("test.torrent").toFile();
        FileInputStream inputStream = new FileInputStream(torrentFile);
        BDecoder reader = new BDecoder(inputStream);
        Map<String, BEncodedValue> document = reader.decodeMap().getMap();
        Map<String, BEncodedValue> info = document.get("info").getMap();
        assertEquals(document.get("announce-list").getList().size(), 6);
        assertEquals(info.get("piece length").getInt(), 131072);
        assertEquals(info.get("length").getInt(), 128161451);
    }

    @Test
    public void testConnectToTracker() throws IOException {

        File torrentFile = Path.of("test.torrent").toFile();
        FileInputStream inputStream = new FileInputStream(torrentFile);
        BDecoder reader = new BDecoder(inputStream);
        Map<String, BEncodedValue> document = reader.decodeMap().getMap();
        String announce = document.get("announce").getString();
        Map<String, BEncodedValue> info = document.get("info").getMap();
        List<String> announces = new ArrayList<>();
        announces.add(announce);
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        List<URI> uriList = GetUriList.getUriList(info, id, announces);
        List<byte[]> bytes = connectToTracker(uriList);
        assertTrue(bytes.size() > 0);
    }

    @Test
    public void testGetInfoHash() throws IOException {

        File torrentFile = Path.of("test.torrent").toFile();
        FileInputStream inputStream = new FileInputStream(torrentFile);
        BDecoder reader = new BDecoder(inputStream);
        Map<String, BEncodedValue> document = reader.decodeMap().getMap();
        Map<String, BEncodedValue> info = document.get("info").getMap();
        byte[] infoHash = new byte[]{20, 9, 40, -90, -47, -27, -26, 24, -82, -52, -110, 38, -93, -39, 90, -105, -122, 118, -48, 34};
        assertArrayEquals(GetHashValue.getHashValue(info), infoHash);
    }

    @Test
    public void testHandshake() throws IOException {

        new BitTorrent(6683, "test.torrent", "online", null, -1);
        File torrentFile = Path.of("test.torrent").toFile();
        FileInputStream inputStream = new FileInputStream(torrentFile);
        BDecoder reader = new BDecoder(inputStream);
        Map<String, BEncodedValue> document = reader.decodeMap().getMap();
        Map<String, BEncodedValue> info = document.get("info").getMap();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        InetSocketAddress localhost = new InetSocketAddress("localhost", 6683);
        Socket socket = new Socket();
        socket.connect(localhost);
        ConnectToPeer.doHandshake(socket,info,id);
        DataInputStream peerMessage = new DataInputStream(socket.getInputStream());
        assertEquals(peerMessage.read(), 19);
        assertArrayEquals(peerMessage.readNBytes("BitTorrent protocol".length()), "BitTorrent protocol".getBytes());
    }

    @Test
    public void testBitFiled() throws IOException {

        new BitTorrent(6684, "test.torrent", "online", null, -1);
        File torrentFile = Path.of("test.torrent").toFile();
        FileInputStream inputStream = new FileInputStream(torrentFile);
        BDecoder reader = new BDecoder(inputStream);
        Map<String, BEncodedValue> document = reader.decodeMap().getMap();
        Map<String, BEncodedValue> info = document.get("info").getMap();
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        InetSocketAddress localhost = new InetSocketAddress("localhost", 6684);
        Socket socket = new Socket();
        socket.connect(localhost);
        ConnectToPeer.doHandshake(socket,info,id);
        DataInputStream peerMessage = new DataInputStream(socket.getInputStream());
        peerMessage.read();
        peerMessage.readNBytes("BitTorrent protocol".length());
        peerMessage.readNBytes(8);
        peerMessage.readNBytes(20);
        peerMessage.readNBytes(20);
        assertTrue(peerMessage.readInt() > 0);
        assertTrue(peerMessage.read()  == 5);
    }
}
