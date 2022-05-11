package bitTorrent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ConnectToTracker {

    private static final Logger LOGGER = LogManager.getLogger(ConnectToTracker.class.getName());

    public static List<byte[]> connectToTracker(List<URI> uriList) {
        //Talk To Tracker
        List<byte[]> bodies = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();
        for (URI uri : uriList) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .build();

            try {
                HttpResponse<byte[]> response =
                        client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                bodies.add(response.body());
                LOGGER.info("Connecting to tracker.");
            } catch (IOException | InterruptedException e) {
                LOGGER.info("Failed to connect to this tracker.");
            }
        }
        return bodies;
    }
}
