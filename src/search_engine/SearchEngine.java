package search_engine;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Moteur de recherche vectoriel.
 * Charge les pages indexées depuis un répertoire et permet d'effectuer
 * des recherches par similarité cosinus entre la requête et les pages.
 */
public class SearchEngine {

    private Path indexDirectory;
    private IndexedPage[] pages;
    private Lemmatizer lemmatizer;

    /**
     * Initialise le moteur en chargeant toutes les pages du répertoire d'index
     * et le lemmatiseur depuis le répertoire des lemmes.
     */
    public SearchEngine(Path indexDirectory, Path lemmasDirectory) throws IOException {
        this.indexDirectory = indexDirectory;
        List<IndexedPage> pageList = new ArrayList<>();

        System.out.println("Chargement de l'index depuis : " + indexDirectory.toAbsolutePath());

        // Parcours de tous les fichiers du répertoire d'index
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(indexDirectory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    pageList.add(new IndexedPage(file));
                }
            }
        }

        this.pages = pageList.toArray(new IndexedPage[0]);
        System.out.println("Nombre de pages chargées : " + this.pages.length);

        this.lemmatizer = new Lemmatizer(
            lemmasDirectory.resolve("dico.txt").toString(),
            lemmasDirectory.resolve("blacklist.txt").toString()
        );
    }

    public IndexedPage getPage(int i) { return pages[i]; }
    public int getPagesNumber()       { return pages.length; }

    /**
     * Effectue une recherche et retourne les résultats dont le score dépasse le seuil,
     * triés par ordre décroissant de pertinence.
     */
    public SearchResult[] search(String query, double threshold) {
        // Transformation de la requête en lemmes avant comparaison
        String lemmatizedQuery = lemmatizer.lemmatizeQuery(query);

        // Si tous les mots sont filtrés (trop courts ou blacklistés), aucun résultat
        if (lemmatizedQuery.isBlank()) {
            return new SearchResult[0];
        }

        // Représentation vectorielle de la requête
        IndexedPage queryVector = new IndexedPage(lemmatizedQuery);

        // Calcul du score de similarité pour chaque page indexée
        double[] scores = new double[pages.length];
        int matchCount = 0;
        for (int i = 0; i < pages.length; i++) {
            scores[i] = queryVector.proximity(pages[i]);
            if (scores[i] > threshold) matchCount++;
        }

        // Construction du tableau de résultats filtrés
        SearchResult[] results = new SearchResult[matchCount];
        int idx = 0;
        for (int i = 0; i < pages.length; i++) {
            if (scores[i] > threshold) {
                // Décodage de l'URL encodée (ex: %C3%A9 → é)
                String url = pages[i].getUrl();
                try { url = URLDecoder.decode(url, "UTF-8"); }
                catch (Exception e) { /* on conserve l'URL brute en cas d'échec */ }
                results[idx++] = new SearchResult(url, scores[i]);
            }
        }

        Arrays.sort(results);
        return results;
    }

    /**
     * Surcharge sans seuil : retourne tous les résultats avec un score supérieur à 0.
     */
    public SearchResult[] search(String query) {
        return search(query, 0.0);
    }

    /**
     * Affiche les 15 premiers résultats d'une recherche dans la console.
     */
    public void printResults(String query) {
        SearchResult[] results = search(query);
        for (int i = 0; i < Math.min(15, results.length); i++) {
            System.out.println(results[i]);
        }
    }

    /**
     * Point d'entrée en ligne de commande.
     * Accepte des mots-clés en argument ou lance un mode interactif si aucun argument.
     */
    public static void main(String[] args) throws Exception {
        URL location = SearchEngine.class.getProtectionDomain().getCodeSource().getLocation();
        Path binFolder    = Paths.get(location.toURI());
        Path indexFolder  = binFolder.getParent().resolve(Paths.get("doc", "exemples-fichiers", "INDEX"));
        Path lemmasFolder = binFolder.getParent().resolve(Paths.get("doc", "exemples-fichiers", "LEMMES"));

        SearchEngine engine = new SearchEngine(indexFolder, lemmasFolder);

        if (args.length > 0) {
            engine.printResults(String.join(" ", args));
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Bienvenue, tapez 'exit' pour quitter.");
            while (true) {
                System.out.print("Recherche : ");
                String line = scanner.nextLine().trim();
                if (line.equals("exit")) { System.out.println("À bientôt !"); break; }
                if (!line.isEmpty()) engine.printResults(line);
            }
            scanner.close();
        }
    }
}