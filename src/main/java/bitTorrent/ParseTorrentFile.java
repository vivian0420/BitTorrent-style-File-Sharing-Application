package bitTorrent;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static bitTorrent.ConnectToPeers.connectToPeers;

public class ParseTorrentFile {

    private static final Logger LOGGER = LogManager.getLogger(ParseTorrentFile.class.getName());

    public static void getTorrentData() {
        File torrentFile = Path.of("/Users/vivianzhang/dsd-final-project-vivian0420/test.torrent").toFile();
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(torrentFile);
            BDecoder reader = new BDecoder(inputStream);
            Map<String, BEncodedValue> document = reader.decodeMap().getMap();
            String announce = document.get("announce").getString();
            LOGGER.info("announce is: " + announce);
            Map<String, BEncodedValue> info = document.get("info").getMap();
            String name = info.get("name").getString();
            Long length = info.get("length").getLong();
            int pieceLength = info.get("piece length").getInt();
            byte [] pieces = info.get("pieces").getBytes();
            List<byte[]> eachPiece = new ArrayList<>();
            DataInputStream piecesIn = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(pieces)));
            while (piecesIn.available() > 0) {
                eachPiece.add(piecesIn.readNBytes(20));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        getTorrentData();
    }
}
