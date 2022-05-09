package bitTorrent;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TalkToTracker {

    private static final Logger LOGGER = LogManager.getLogger(TalkToTracker.class.getName());
    final static String peerId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);

    public static String getInfoHash(Map<String, BEncodedValue> info) {
        MessageDigest hash;
        String infoHash = null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BEncoder.encode(info, outputStream);
            hash = MessageDigest.getInstance("SHA-1");
            byte[] hashValue = hash.digest(outputStream.toByteArray());
            infoHash = URLEncoder.encode(new String(hashValue, StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);

        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        return infoHash;
    }

    /* https://wiki.theory.org/BitTorrentSpecification#Info_in_Multiple_File_Mode */
    public static List<URI> getURL (Map<String, BEncodedValue> info, Long length, List<String> announces) {
        String infoHash = getInfoHash(info);
        //String peerId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String port = String.valueOf(6881);
        String left = String.valueOf(length);
        String downloaded = String.valueOf(0L);
        String uploaded = String.valueOf(0L);

        List<URI> uriList = new ArrayList<>();
        for (String announce: announces) {
            URI uri = URI.create(announce + "?" + "info_hash=" + infoHash + "&peer_id=" + peerId + "&port=" + port + "&left=" + left + "&downloaded=" + downloaded + "&uploaded=" + uploaded);
            uriList.add(uri);
        }
        return uriList;
    }

    public static byte[] getTrackerResponse(Map<String, BEncodedValue> info, Long length, List<String> announces) {
        List<URI> uriList = getURL(info, length, announces);
        byte[]  body = new byte[0];
        HttpClient client = HttpClient.newHttpClient();
        for(URI uri: uriList) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .build();
            try {
                HttpResponse<byte[]> response =
                        client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                return response.body();

            } catch (IOException | InterruptedException e) {
                LOGGER.info("Failed to connect to Tracker");
            }
        }
        return body;
    }
}
