package herve_web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import search_engine.SearchEngine;
import search_engine.SearchResult;

class RequestHandler implements HttpHandler {

    private final SearchEngine engine;
    private final int          maxResults;
    private final double       threshold;
    private final Path         assetsDir;

    public RequestHandler(Path indexDir, Path lemmasDir, int maxResults, double threshold) throws IOException {
        this.assetsDir  = Paths.get("src", "assets");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath  = exchange.getRequestURI().getPath();
        String queryString  = exchange.getRequestURI().getQuery();

        // Servir les fichiers CSS statiques
        if (requestPath.endsWith(".css")) {
            serveStaticFile(exchange, requestPath.substring(1), "text/css; charset=UTF-8");
            return;
        }
        if (requestPath.endsWith(".svg")) {
            serveStaticFile(exchange, requestPath.substring(1), "image/svg+xml");
            return;
        }

        // Générer la page HTML de résultats
        String responseBody = buildHtmlResponse(queryString);
        sendHtmlResponse(exchange, responseBody);
    }

        try {
            Path template = assetsDir.resolve("index.html");
            String html = new String(Files.readAllBytes(template), StandardCharsets.UTF_8);

            StringBuilder htmlHistory = new StringBuilder();
            if (!history.isEmpty()) {
                // Utilisation des classes CSS de style.css (.suggestions, .suggestion-label, .suggestion-tag)
                htmlHistory.append("<div class=\"suggestions\">");
                htmlHistory.append("<span class=\"suggestion-label\">Dernières recherches :</span>");
                
                int count = 0;
                for (int i = history.size() - 1; i >= 0 && count < 5; i--) {
                    String h = history.get(i);
                    String encoded = URLEncoder.encode(h, StandardCharsets.UTF_8);
                    htmlHistory.append(String.format("<a href=\"/resultats.html?search=%s\" class=\"suggestion-tag\">%s</a>", encoded, h));
                    count++;
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
            Path template = assetsDir.resolve("resultats.html");
            String html = new String(Files.readAllBytes(template), StandardCharsets.UTF_8);


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