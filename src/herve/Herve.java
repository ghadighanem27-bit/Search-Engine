package herve;

import herve_web.WebServer; // Import de WebServer
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import search_engine.SearchEngine;
import search_engine.SearchResult;

/**
 * Point d'entrée principal du moteur de recherche Hervé.
 * Gère trois commandes : ask (recherche directe), run (mode interactif), web (serveur HTTP).
 */
public class Herve {

    private static final Path DEFAULT_INDEX_DIR = Paths.get(System.getProperty("user.home"), ".config", "herve", "INDEX");
    private static final int    DEFAULT_MAX       = Integer.MAX_VALUE;
    private static final double DEFAULT_THRESHOLD = 0.0;
    private static final int    DEFAULT_PORT      = 2026;

    public static void main(String[] args) {
        if (args.length < 1) {
            printHelp();
            return;
        }

        String command       = args[0];
        String[] remaining   = Arrays.copyOfRange(args, 1, args.length);

        switch (command) {
            case "ask": ask(remaining); break;
            case "run": run(remaining); break;
            case "web": web(remaining); break;
            default:
                System.out.println("Commande inconnue : " + command);
                printHelp();
        }
    }

    // ------------------------------------------------------------------ //
    //  Lecture des options en ligne de commande                           //
    // ------------------------------------------------------------------ //

    /**
     * Récupère le chemin du répertoire d'index depuis les arguments, ou retourne le chemin par défaut.
     */
    private static Path parseIndexDir(String[] args) {
        for (int i = 0; i < args.length - 1; i++)
            if (args[i].equals("--index")) return Paths.get(args[i + 1]);
        return DEFAULT_INDEX_DIR;
    }

    /**
     * Cherche le répertoire des lemmes en testant plusieurs emplacements courants.
     */
    private static Path parseLemmasDir(Path indexDir) {
        Path[] candidates = {
            Paths.get("doc", "exemples-fichiers", "LEMMES"),
            indexDir.getParent() != null ? indexDir.getParent().resolve("LEMMES") : null,
            Paths.get(System.getProperty("user.home"), ".config", "herve", "LEMMES")
        };
        for (Path candidate : candidates) {
            if (candidate != null && Files.exists(candidate.resolve("dico.txt"))) return candidate;
        }
        return Paths.get("doc", "exemple-fichiers", "LEMMES");
    }

    /**
     * Récupère le nombre maximum de résultats à afficher depuis les arguments.
     */
    private static int parseMax(String[] args) {
        for (int i = 0; i < args.length - 1; i++)
            if (args[i].equals("--max")) {
                try { return Integer.parseInt(args[i + 1]); }
                catch (NumberFormatException e) { /* valeur invalide ignorée */ }
            }
        return DEFAULT_MAX;
    }

    /**
     * Récupère le seuil minimum de score depuis les arguments.
     */
    private static double parseThreshold(String[] args) {
        for (int i = 0; i < args.length - 1; i++)
            if (args[i].equals("--seuil")) {
                try { return Double.parseDouble(args[i + 1]); }
                catch (NumberFormatException e) { /* valeur invalide ignorée */ }
            }
        return DEFAULT_THRESHOLD;
    }

    /**
     * Récupère le port du serveur web depuis les arguments.
     */
    private static int parsePort(String[] args) {
        for (int i = 0; i < args.length - 1; i++)
            if (args[i].equals("--port")) {
                try { return Integer.parseInt(args[i + 1]); }
                catch (NumberFormatException e) { /* valeur invalide ignorée */ }
            }
        return DEFAULT_PORT;
    }

    /**
     * Récupère le terme de recherche initial pour la commande web depuis les arguments.
     */
    private static String parseInitialSearch(String[] args) {
        for (int i = 0; i < args.length - 1; i++)
            if (args[i].equals("--recherche")) return args[i + 1];
        return "";
    }

    /**
     * Assemble les mots-clés de la requête depuis les arguments, en ignorant les options (--xxx).
     */
    private static String parseQuery(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) { i++; continue; }
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ //
    //  Commandes                                                           //
    // ------------------------------------------------------------------ //

    /**
     * Lance une recherche directe depuis la ligne de commande et affiche les résultats.
     */
    private static void ask(String[] args) {
        Path   indexDir  = parseIndexDir(args);
        Path   lemmasDir = parseLemmasDir(indexDir);
        int    max       = parseMax(args);
        double threshold = parseThreshold(args);
        String query     = parseQuery(args);

        if (query.isBlank()) {
            System.out.println("Veuillez fournir des mots-clés. Exemple : herve ask tomate oignon");
            return;
        }

        try {
            validateIndexDir(indexDir);
            SearchEngine engine = new SearchEngine(indexDir, lemmasDir);
            printResults(engine, query, max, threshold);
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    /**
     * Lance le moteur en mode interactif : l'utilisateur saisit ses requêtes une par une.
     */
    private static void run(String[] args) {
        Path   indexDir  = parseIndexDir(args);
        Path   lemmasDir = parseLemmasDir(indexDir);
        int    max       = parseMax(args);
        double threshold = parseThreshold(args);

        try {
            validateIndexDir(indexDir);
            SearchEngine engine = new SearchEngine(indexDir, lemmasDir);
            System.out.println("Répertoire d'index : " + indexDir.toAbsolutePath());
            System.out.println("Bienvenue sur Hervé, moteur de recherche vectoriel. Tapez 'exit' pour quitter.");

            java.util.Scanner scanner = new java.util.Scanner(System.in);
            while (true) {
                System.out.print("Recherche : ");
                String line = scanner.nextLine().trim();
                if (line.equals("exit")) { System.out.println("À bientôt !"); break; }
                if (!line.isEmpty()) printResults(engine, line, max, threshold);
            }
            scanner.close();
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    /**
     * Lance le serveur HTTP et ouvre automatiquement le navigateur.
     */
    private static void web(String[] args) {
        Path   indexDir     = parseIndexDir(args);
        Path   lemmasDir    = parseLemmasDir(indexDir);
        int    max          = parseMax(args);
        double threshold    = parseThreshold(args);
        int    port         = parsePort(args);
        String initialQuery = parseInitialSearch(args);

        try {
            validateIndexDir(indexDir);

            if (!isServerRunning(port)) {
                // Utilisation de WebServer
                WebServer.start(indexDir, lemmasDir, max, threshold, port);
            } else {
                System.out.println("Serveur déjà actif sur le port " + port);
            }

            String url = initialQuery.isBlank()
                ? "http://localhost:" + port
                : "http://localhost:" + port + "/?recherche=" + java.net.URLEncoder.encode(initialQuery, "UTF-8");

            System.out.println("Ouvrez : " + url);
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            }
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    private static boolean isServerRunning(int port) {
        try {
            java.net.URL url = java.net.URI.create("http://127.0.0.1:" + port + "/test").toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(500);
            conn.setReadTimeout(500);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code == 200) {
                java.util.Scanner s = new java.util.Scanner(conn.getInputStream());
                String body = s.useDelimiter("\\A").next();
                s.close();
                return "OK".equals(body.trim());
            }
        } catch (Exception e) {
            // Pas de réponse = serveur inactif
        }
        return false;
    }

    // ------------------------------------------------------------------ //
    //  Utilitaires                                                         //
    // ------------------------------------------------------------------ //

    public static void printResults(SearchEngine engine, String query, int max, double threshold) {
        SearchResult[] results = engine.search(query, threshold);
        int total     = results.length;
        int displayed = Math.min(max, total);

        System.out.println(total + " résultat(s) pour \"" + query + "\".");
        for (int i = 0; i < displayed; i++) {
            System.out.println(results[i]);
        }
        if (total > displayed) {
            System.out.println((total - displayed) + " résultat(s) supplémentaire(s) non affiché(s).");
        }
    }

    /**
     * Vérifie que le répertoire d'index existe et est bien un dossier.
     */
    private static void validateIndexDir(Path indexDir) {
        if (!Files.exists(indexDir))
            throw new IllegalArgumentException("Le répertoire d'index n'existe pas : " + indexDir.toAbsolutePath());
        if (!Files.isDirectory(indexDir))
            throw new IllegalArgumentException("Le chemin indiqué n'est pas un répertoire : " + indexDir.toAbsolutePath());
    }

    /**
     * Affiche le message d'aide avec les commandes disponibles.
     */
    private static void printHelp() {
        System.out.println("Utilisation : herve <commande> [options]");
        System.out.println("Commandes :");
        System.out.println("  ask [--index <rép>] [--max <n>] [--seuil <s>] <mots clés>");
        System.out.println("  run [--index <rép>] [--max <n>] [--seuil <s>]");
        System.out.println("  web [--index <rép>] [--max <n>] [--seuil <s>] [--port <p>] [--recherche <requête>]");
    }
}