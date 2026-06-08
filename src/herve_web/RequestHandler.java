package herve_web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import search_engine.SearchEngine;
import search_engine.SearchResult;

/**
 * Gestionnaire des requêtes HTTP du serveur Hervé.
 * Traite les fichiers CSS statiques et génère dynamiquement les pages HTML de résultats.
 */
class RequestHandler implements HttpHandler {

    private final SearchEngine engine;
    private final int          maxResults;
    private final double       threshold;
    private final Path         resourcesDir; // chemin absolu vers resources/

    public RequestHandler(Path indexDir, Path lemmasDir, int maxResults, double threshold) throws IOException {
        this.engine      = new SearchEngine(indexDir, lemmasDir);
        this.maxResults  = maxResults;
        this.threshold   = threshold;

        // Résolution robuste : remonte depuis le .class jusqu'à la racine du projet
        java.net.URL location = RequestHandler.class.getProtectionDomain().getCodeSource().getLocation();
        
        Path binFolder;
        try {
            binFolder = Paths.get(location.toURI());
        } catch (URISyntaxException e) {
            // On convertit l'erreur en IOException pour respecter la signature du constructeur
            throw new IOException("Erreur lors de la conversion de l'URL en URI", e);
        }
        
        this.resourcesDir = binFolder.getParent().resolve("resources");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath  = exchange.getRequestURI().getPath();
        String queryString  = exchange.getRequestURI().getQuery();

        // Servir les fichiers CSS statiques
        if (requestPath.endsWith(".css")) {
            serveCssFile(exchange, requestPath);
            return;
        }

        // Générer la page HTML de résultats
        String responseBody = buildHtmlResponse(queryString);
        sendHtmlResponse(exchange, responseBody);
    }

    /**
     * Lit et envoie un fichier CSS depuis le répertoire source.
     */
    private void serveCssFile(HttpExchange exchange, String requestPath) throws IOException {
        try {
            Path cssPath = resourcesDir.resolve(requestPath.substring(1));
            if (Files.exists(cssPath)) {
                byte[] cssBytes = Files.readAllBytes(cssPath);
                exchange.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
                exchange.sendResponseHeaders(200, cssBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(cssBytes);
                }
            }
        } catch (Exception e) {
            // Si le fichier CSS est introuvable, on laisse le navigateur gérer l'absence de style
        }
    }

    /**
     * Construit la page HTML de résultats en remplissant le template avec les données de la recherche.
     */
    private String buildHtmlResponse(String queryString) {
        try {
            Path htmlTemplate = resourcesDir.resolve("resultat.html");
            String html = new String(Files.readAllBytes(htmlTemplate), StandardCharsets.UTF_8);

            String searchTerm   = "";
            String resultsBlock = "<p>Entrez un mot-clé pour lancer la recherche.</p>";

            if (queryString != null) {
                searchTerm   = extractSearchTerm(queryString);
                resultsBlock = buildResultsBlock(searchTerm);
            }

            // Injection du terme de recherche et des résultats dans le template HTML
            return html.replace("{{REQUETE}}", searchTerm)
                       .replace("{{RESULTATS}}", resultsBlock);

        } catch (Exception e) {
            return "<h1>Erreur</h1><p>" + e.getMessage() + "</p>";
        }
    }

    /**
     * Extrait et décode le terme de recherche depuis la chaîne de paramètres GET.
     */
    private String extractSearchTerm(String queryString) {
        for (String segment : queryString.split("&")) {
            if (segment.startsWith("recherche=")) {
                String rawValue = segment.substring("recherche=".length());
                try {
                    return URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return rawValue;
                }
            }
        }
        return "";
    }

    /**
     * Lance la recherche et construit le bloc HTML listant les résultats.
     */
    private String buildResultsBlock(String searchTerm) {
        if (searchTerm.trim().isEmpty()) {
            return "<p>Entrez un mot-clé pour lancer la recherche.</p>";
        }

        SearchResult[] results   = engine.search(searchTerm, threshold);
        int total                = results.length;
        int displayed            = Math.min(maxResults, total);

        if (total == 0) {
            return "<p>Aucun document trouvé pour : <b>" + searchTerm + "</b></p>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<p>").append(total)
          .append(" résultat(s) pour <b>").append(searchTerm).append("</b></p>");
        sb.append("<ul>");
        for (int i = 0; i < displayed; i++) {
            String url   = results[i].getUrl();
            double score = results[i].getScore();
            sb.append("<li>")
              .append("<a href=\"").append(url).append("\" target=\"_blank\">").append(url).append("</a>")
              .append(" — score : ").append(String.format("%.4f", score))
              .append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    /**
     * Envoie la réponse HTML encodée en UTF-8 au client.
     */
    private void sendHtmlResponse(HttpExchange exchange, String body) throws IOException {
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}