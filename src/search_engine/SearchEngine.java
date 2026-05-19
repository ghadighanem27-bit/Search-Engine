//revoir les fonctions si elle respect la SAE


package search_engine;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchEngine {

    private IndexedPage[] pages;
    private Path indexationDirectory;

    // Charge tous les fichiers d'index depuis le dossier donné
    public SearchEngine(Path indexationDirectory) throws IOException {
        this.indexationDirectory = indexationDirectory; // Assignation ajoutée
        List<IndexedPage> liste = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(indexationDirectory, "*.txt")) {
            for (Path fichier : stream) {
                liste.add(new IndexedPage(fichier));
            }
        }
        this.pages = liste.toArray(new IndexedPage[0]);
    }

    // Calcule et retourne les résultats triés pour une requête
    public List<SearchResult> getResults(String requestString) {
        IndexedPage requete = new IndexedPage(requestString);
        List<SearchResult> resultats = new ArrayList<>();

        for (IndexedPage page : pages) {
            double score = requete.proximity(page);
            if (score > 0) {
                resultats.add(new SearchResult(page.getUrl(), score));
            }
        }

        Collections.sort(resultats);
        return resultats;
    }

    // Retourne la page à l'index i
    public IndexedPage getPage(int i) {
        if (i >= 0 && i < pages.length) {
            return pages[i];
        }
        throw new IndexOutOfBoundsException("L'index " + i + " est hors limites.");
    }

    // Retourne le nombre total de pages indexées
    public int getPagesNumber() {
        return pages.length;
    }

    // Lance la requête et retourne un tableau de résultats
    public SearchResult[] launchRequest(String requeString) {
        List<SearchResult> resultats = getResults(requeString);
        return resultats.toArray(new SearchResult[0]);
    }

    // Affiche les 15 meilleurs résultats
    public void printResults(String requestString) {
        List<SearchResult> resultats = getResults(requestString);
        int max = Math.min(15, resultats.size());
        for (int i = 0; i < max; i++) {
            System.out.println(resultats.get(i));
        }
    }
}