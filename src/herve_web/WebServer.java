package herve_web;

import com.sun.net.httpserver.HttpServer; // Aucun conflit de nom avec herve_web.WebServer !
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Démarre le serveur HTTP embarqué du moteur de recherche Hervé.
 */
public class WebServer {

    /**
     * Crée et lance le serveur sur le port indiqué.
     * Le RequestHandler est instancié ici : les fichiers d'index sont chargés une seule fois au démarrage.
     */
    public static void start(Path indexDir, Path lemmasDir, int maxResults, double threshold, int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RequestHandler(indexDir, lemmasDir, maxResults, threshold));

        // Endpoint de vérification : répond "OK" si le serveur tourne
        server.createContext("/test", exchange -> {
            byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        server.start();
        System.out.println("Serveur démarré sur le port " + port);
    }
}