package herve_web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import search_engine.SearchEngine;
import search_engine.SearchResult;
import search_engine.WandSearchEngine;

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
        String requestPath = exchange.getRequestURI().getPath();
        String queryString = exchange.getRequestURI().getQuery();

        // Gestion des fichiers statiques (CSS, images, etc.)
        if (requestPath.endsWith(".css")) {
            serveStaticFile(exchange, requestPath.substring(1), "text/css; charset=UTF-8");
            return;
        }
        if (requestPath.endsWith(".svg")) {
            serveStaticFile(exchange, requestPath.substring(1), "image/svg+xml");
            return;
        }

        // Page d'accueil (index.html)
        if (requestPath.equals("/") || requestPath.equals("/index.html")) {
            String searchTerm = extractParam(queryString, "search").trim();
            if (searchTerm.isEmpty()) {
                searchTerm = extractParam(queryString, "q").trim();
            }
            
            if (!searchTerm.isEmpty()) {
                String responseBody = buildResultsPage(queryString);
                sendResponse(exchange, responseBody, "text/html; charset=UTF-8");
            } else {
                String responseBody = buildIndexPage();
                sendResponse(exchange, responseBody, "text/html; charset=UTF-8");
            }
            return;
        }

        // Page de résultats (resultats.html)
        String responseBody = buildResultsPage(queryString);
        sendResponse(exchange, responseBody, "text/html; charset=UTF-8");
    }

    private void serveStaticFile(HttpExchange exchange, String relativePath, String contentType) throws IOException {
        Path filePath = assetsDir.resolve(relativePath);
        if (!Files.exists(filePath)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        byte[] bytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String buildIndexPage() {
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
            return "<h1>Erreur</h1><p>" + e.getMessage() + "</p>";
        }
    }

    private String buildResultsPage(String queryString) {
        try {
            Path template = assetsDir.resolve("resultats.html");
            String html = new String(Files.readAllBytes(template), StandardCharsets.UTF_8);

            // Récupération du terme recherché via 'search' ou 'q'
            String searchTerm = extractParam(queryString, "search").trim();
            if (searchTerm.isEmpty()) {
                searchTerm = extractParam(queryString, "q").trim();
            }

            String searchTerm   = "";
            String resultsBlock = "<p>Entrez un mot-clé pour lancer la recherche.</p>";

            String resultsHTML;
            if (searchTerm.isEmpty()) {
                resultsHTML = "<p style=\"color: var(--muted); font-style: italic;\">Entrez un mot-clé pour lancer la recherche.</p>";
            } else {
                resultsHTML = buildResultsBlocks(searchTerm);
            }

            // Remplacement des tags {{QUERY}} et {{RESULTS}} dans le fichier resultats.html
            html = html.replace("{{QUERY}}", searchTerm);
            html = html.replace("{{RESULTS}}", resultsHTML);
            
            return html;

        } catch (Exception e) {
            return "<h1>Erreur</h1><p>" + e.getMessage() + "</p>";
        }
    }

    private String buildResultsBlocks(String searchTerm) {
        SearchResult[] results = engine.search(searchTerm, threshold);
        
        if (results == null || results.length == 0) {
            return "<p style=\"color: var(--muted); font-style: italic;\">Aucun document trouvé pour : " + searchTerm + "</p>";
        }

        int total = results.length;
        int displayed = Math.min(maxResults, total);

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-size: 14px; color: var(--muted); margin-bottom: 20px;\">")
          .append(total).append(" résultat(s) trouvé(s)</div>\n\n");

        for (int i = 0; i < displayed; i++) {
            String url = results[i].getUrl();
            double score = results[i].getScore();

            String title = url;
            if (url.contains("/wiki/")) {
                String rawTitle = url.substring(url.indexOf("/wiki/") + 6);
                try {
                    title = URLDecoder.decode(rawTitle, StandardCharsets.UTF_8).replace("_", " ");
                } catch (Exception e) {
                    title = rawTitle.replace("_", " ");
                }
            }

            // Restauration de l'affichage complet d'origine (div, liens, scores)
            sb.append("<div class=\"result-block\" style=\"margin-bottom: 24px; font-family: 'DM Sans', sans-serif;\">\n");
            sb.append(String.format("  <div class=\"result-url-preview\" style=\"font-size: 12px; color: var(--muted); margin-bottom: 1px; word-break: break-all;\">%s</div>\n", url));
            sb.append(String.format("  <a href=\"%s\" target=\"_blank\" style=\"display: inline-block !important; padding: 0 !important; margin: 0 !important; font-size: 18px; color: var(--blue); text-decoration: none; font-weight: 500; line-height: 1.3;\">%s</a>\n", url, title));
            sb.append(String.format("  <div class=\"result-score-desc\" style=\"font-size: 13px; color: #475569; margin-top: 4px;\">Score de pertinence : <strong>%.4f</strong></div>\n", score));
            sb.append("</div>\n");
        }
        
        return sb.toString();
    }

    private String extractParam(String queryString, String param) {
        if (queryString == null) return "";
        for (String segment : queryString.split("&")) {
            if (segment.startsWith(param + "=")) {
                String raw = segment.substring(param.length() + 1);
                try { return URLDecoder.decode(raw, StandardCharsets.UTF_8); }
                catch (Exception e) { return raw; }
            }
        }
        return "";
    }

    private void sendResponse(HttpExchange exchange, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}