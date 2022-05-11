package utility;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class GetHashValue {

    public static byte[] getHashValue(Map<String, BEncodedValue> info) {
        //get info hash value
        MessageDigest hash;
        byte[] hashValue = new byte[0];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BEncoder.encode(info, outputStream);
            hash = MessageDigest.getInstance("SHA-1");
            hashValue = hash.digest(outputStream.toByteArray());

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashValue;
    }
}
