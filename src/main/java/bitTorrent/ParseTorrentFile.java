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
import java.util.List;
import java.util.Map;



public class ParseTorrentFile {

    private static final Logger LOGGER = LogManager.getLogger(ParseTorrentFile.class.getName());

    public static Map<String, BEncodedValue> parseTorrentFile(List<byte[]> eachPiece, List<String> announces) {

        //Parse torrent file
        File torrentFile = Path.of("/Users/vivianzhang/Downloads/[SubsPlease] Yuusha, Yamemasu - 05 (720p) [5D2E9073].mkv.torrent").toFile();
        FileInputStream inputStream = null;
        Map<String, BEncodedValue> info = null;
        try {
            inputStream = new FileInputStream(torrentFile);
            BDecoder reader = new BDecoder(inputStream);
            Map<String, BEncodedValue> document = reader.decodeMap().getMap();
            String announce = document.get("announce").getString();
            List<BEncodedValue> announceList = document.get("announce-list").getList();

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
            byte[] pieces = info.get("pieces").getBytes();
            DataInputStream piecesIn = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(pieces)));
            while (piecesIn.available() > 0) {
                eachPiece.add(piecesIn.readNBytes(20));
            }
            LOGGER.info("eachPiece's size = " + eachPiece.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return info;
    }
}
