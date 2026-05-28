package search_engine;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class SearchEngine {

    private Path indexationDirectory;
    private IndexedPage[] pages;
    private Lemmatisation lemmatiseur;

    public SearchEngine(Path indexationDirectory, Path lemmesDirectory) throws IOException {
        this.indexationDirectory = indexationDirectory;

        // On utilise une liste temporaire car DirectoryStream ne nous donne pas 
        // la taille totale à l'avance.
        List<IndexedPage> list = new ArrayList<>();

        // Les fichiers d'INDEX_FILES peuvent ne pas avoir d'extension.
        // On parcourt donc tout le dossier puis on garde uniquement les fichiers réguliers.
        // L'utilisation du bloc "try-with-resources" permet de fermer le flux automatiquement.
        // newDirectoryStream lève directement une IOException (ou NotDirectoryException) si le dossier est invalide.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(indexationDirectory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    list.add(new IndexedPage(file));
                }
            }
        }
        this.pages = list.toArray(new IndexedPage[0]);

        // Chargement du lemmatiseur depuis le dossier LEMMES
        this.lemmatiseur = new Lemmatisation(
            lemmesDirectory.resolve("dico.txt").toString(),
            lemmesDirectory.resolve("blacklist.txt").toString()
        );
    }

    public IndexedPage getPage(int i) {
        return pages[i];
    }

    public int getPagesNumber() {
        return pages.length;
    }

    public SearchResult[] launchRequest(String requestString) {
        // On crée une IndexedPage temporaire à partir de la requête de l'utilisateur.
        // Cela nous permet de réutiliser directement la méthode proximity() déjà définie
        // dans IndexedPage, qui calcule la similarité entre deux pages.
        // On lemmatise la requête avant de la traiter
        String requeteLemmatisee = lemmatiseur.lemmatizeQuery(requestString);
        if (requeteLemmatisee.isBlank()) {
            return new SearchResult[0];
        }
        IndexedPage request = new IndexedPage(requeteLemmatisee);

        double[] scores = new double[pages.length];
        int resultCount = 0;
        for (int i = 0; i < pages.length; i++) {
            scores[i] = request.proximity(pages[i]) * pages.length; // modification ici : le score utilisais proximity brut (entre 0 et 1) au lieu de proximity * pages.length

            // On compare le score avec un epsilon afin d'éviter le problème de précision des doubles
            if (scores[i] > 1e-10) {
                resultCount++;
            }
        }

        // On construit le tableau final uniquement avec les pages ayant un score significatif
        SearchResult[] results = new SearchResult[resultCount];
        int resultIndex = 0;
        for (int i = 0; i < pages.length; i++) {
            if (scores[i] > 1e-10) {
                results[resultIndex] = new SearchResult(pages[i].getUrl(), scores[i]);
                resultIndex++;
            }
        }

        // On utilise Arrays.sort() qui utilise la méthode compareTo redéfinie dans la classe SearchResult
        Arrays.sort(results);
        return results;
    }

    public void printResults(String requestString) {
        SearchResult[] results = launchRequest(requestString);

        // Math.min() nous permet de ne jamais afficher plus de 15 résultats,
        // tout en gérant le cas où il y en aurait moins sans risquer un ArrayIndexOutOfBoundsException.
        int max = Math.min(15, results.length);
        for (int i = 0; i < max; i++) {
            System.out.println(results[i]);
        }
    }

    public static void main(String[] args) throws Exception {

        URL location = SearchEngine.class.getProtectionDomain().getCodeSource().getLocation();
        Path binFolder = Paths.get(location.toURI());
        Path indexFolder = binFolder.getParent().resolve("doc/exemples-fichiers/INDEX_FILES");
        Path lemmesFolder = binFolder.getParent().resolve("doc/exemples-fichiers/LEMMES");

        // On passe les deux dossiers au constructeur
        SearchEngine se = new SearchEngine(indexFolder, lemmesFolder);

        if (args.length > 0) {
            // Mode one-shot : arguments interprétés comme des requêtes
            // String.join() assemble tous les arguments de la ligne de commande en une seule
            // chaîne séparée par des espaces.
            String request = String.join(" ", args);
            se.printResults(request);
        } else {
            // Mode interactif : on lit les requêtes au clavier jusqu'à ce que
            // l'utilisateur tape "exit"
            Scanner scanner = new Scanner(System.in);
            System.out.println("Bienvenue, tapez 'exit' pour quitter.");
            while (true) {
                System.out.print("Recherche : ");
                String line = scanner.nextLine().trim();
                if (line.equals("exit")) {
                    System.out.println("À bientôt !");
                    break;
                }
                if (!line.isEmpty()) {
                    se.printResults(line);
                }
            }
            scanner.close();
        }
    }
}
// Pour exécuter, se placer dans le dossier bin et lancer :
// java -cp . search_engine.SearchEngine cerise flan