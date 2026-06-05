package herve_web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

/**
 * Démarre le serveur HTTP embarqué du moteur de recherche Hervé.
 */
public class HttpServer {

    /**
     * Crée et lance le serveur sur le port indiqué.
     * Le RequestHandler est instancié ici : les fichiers d'index sont chargés une seule fois au démarrage.
     */
    public static void start(Path indexDir, Path lemmasDir, int maxResults, double threshold, int port) throws IOException {
        com.sun.net.httpserver.HttpServer server =
            com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RequestHandler(indexDir, lemmasDir, maxResults, threshold));
        server.start();
        System.out.println("Serveur démarré sur le port " + port);
    }
}