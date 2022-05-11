package utility;

import be.adaxisoft.bencode.BEncodedValue;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class BuildIHave {

    public static void buildIHave(Map<String, BEncodedValue> info, List<byte[]> eachPiece, BitSet iHave, int port) {

        // build iHave to support "resume download" and "send bitfield".
        try (DataInputStream in = new DataInputStream(new FileInputStream(Path.of("target", String.valueOf(port), info.get("name").getString()).toFile()))) {
            for (int i = 0; i < eachPiece.size(); i++) {
                int pieceLength = info.get("piece length").getInt();
                if (in.available() > pieceLength) {
                    if (Arrays.equals(MessageDigest.getInstance("SHA-1").digest(in.readNBytes(pieceLength)), eachPiece.get(i))) {
                        iHave.set(i);
                    }
                } else {
                    if (Arrays.equals(MessageDigest.getInstance("SHA-1").digest(in.readAllBytes()), eachPiece.get(i))) {
                        iHave.set(i);
                    }
                }
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
