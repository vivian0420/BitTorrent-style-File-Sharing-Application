package bitTorrent;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.InvalidBEncodingException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static utility.GetHashValue.getHashValue;

public class GetUriList {

    static List<URI> getUriList(Map<String, BEncodedValue> info, String peerId, List<String> announces) throws InvalidBEncodingException {

        byte[] hashValue = getHashValue(info);
        String infoHash = URLEncoder.encode(new String(hashValue, StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);


        //getURI
        String port = String.valueOf(6881);
        String left = String.valueOf(info.get("length").getLong());
        String downloaded = String.valueOf(0L);
        String uploaded = String.valueOf(0L);

        List<URI> uriList = new ArrayList<>();
        for (String eachAnnounce : announces) {
            URI uri = URI.create(eachAnnounce + "?" + "info_hash=" + infoHash + "&peer_id=" + peerId + "&port=" + port + "&left=" + left + "&downloaded=" + downloaded + "&uploaded=" + uploaded + "&compact=1");
            uriList.add(uri);
        }
        return uriList;
    }
}
