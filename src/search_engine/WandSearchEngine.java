package search_engine;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Version optimisée du moteur de recherche avec l'algorithme WAND.
 *
 * Le moteur de base regarde toutes les pages une par une pour chaque recherche.
 * WAND fait la même chose mais en sautant les pages qui ne peuvent pas être
 * dans les meilleurs résultats — sans même calculer leur score.
 */
public class WandSearchEngine extends SearchEngine {


    /**
     * Représente la présence d'un mot dans une page.
     * On retient le numéro de la page et à quel point ce mot y est important.
     */
    private static class Presence {
        final int    pageNumber;
        final double importance;  // occurrences du mot / longueur de la page
        Presence(int pageNumber, double importance) {
            this.pageNumber = pageNumber;
            this.importance = importance;
        }
    }

    /**
     * Liste de toutes les pages qui contiennent un mot donné.
     * Triée par numéro de page, avec un curseur qui avance au fil de la recherche.
     *
     * Contient aussi le "score max possible" : le meilleur score que ce mot
     * peut donner à n'importe quelle page. Ça permet à WAND d'éliminer des
     * pages sans les regarder vraiment.
     */
    private static class PageList {
        private final List<Presence> presences        = new ArrayList<>();
        private double               maxScorePossible = 0.0;
        private int                  cursor           = 0;

        void add(int pageNumber, double importance) {
            presences.add(new Presence(pageNumber, importance));
            if (importance > maxScorePossible) maxScorePossible = importance;
        }

        /** Trie par numéro de page. Appeler une fois quand toutes les pages sont ajoutées. */
        void finalizeList() {
            presences.sort(Comparator.comparingInt(p -> p.pageNumber));
        }

        void    reset()               { cursor = 0; }
        boolean isExhausted()         { return cursor >= presences.size(); }
        int     getPagesCount()       { return presences.size(); }
        double  getMaxScorePossible() { return maxScorePossible; }

        /** Numéro de la page pointée par le curseur. MAX_VALUE si la liste est épuisée. */
        int currentPage() {
            return isExhausted() ? Integer.MAX_VALUE : presences.get(cursor).pageNumber;
        }

        /**
         * Avance le curseur jusqu'à la première page avec un numéro >= cible.
         * Dichotomie : on coupe la liste en deux à chaque étape
         * au lieu d'avancer case par case — beaucoup plus rapide sur de longues listes.
         */
        void advanceTo(int target) {
            int lo = cursor, hi = presences.size();
            while (lo < hi) {
                int mid = (lo + hi) / 2;
                if (presences.get(mid).pageNumber < target) lo = mid + 1;
                else                                         hi = mid;
            }
            cursor = lo;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Index inversé
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * L'index inversé : pour chaque mot, on sait dans quelles pages il apparaît.
     * C'est l'inverse de l'index normal (page → liste de mots).
     * Exemple : "salade" → [page 3, page 17, page 42, ...]
     */
    private final Map<String, PageList> invertedIndex = new HashMap<>();

    private void buildIndex(IndexedPage[] pages) {
        for (int pageNumber = 0; pageNumber < pages.length; pageNumber++) {
            IndexedPage page = pages[pageNumber];
            double pageLength = page.getNorm();
            if (pageLength == 0) continue;

            for (String word : page.getWords()) {
                invertedIndex.computeIfAbsent(word, w -> new PageList())
                             .add(pageNumber, page.getCount(word) / pageLength);
            }
        }
        for (PageList list : invertedIndex.values()) list.finalizeList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Constructeur
    // ══════════════════════════════════════════════════════════════════════════

    private final Lemmatizer lemmatizer;

    public WandSearchEngine(Path indexDirectory, Path lemmasDirectory) throws IOException {
        super(indexDirectory, lemmasDirectory);

        // On reconstruit un Lemmatizer avec le même chemin que SearchEngine
        this.lemmatizer = new Lemmatizer(
            lemmasDirectory.resolve("dico.txt").toString(),
            lemmasDirectory.resolve("blacklist.txt").toString()
        );

        // On construit l'index inversé depuis les pages déjà chargées par le parent
        IndexedPage[] pages = new IndexedPage[getPagesNumber()];
        for (int i = 0; i < pages.length; i++) pages[i] = getPage(i);
        buildIndex(pages);

        System.out.println("Index inversé construit : " + getPagesNumber() + " pages.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Algorithme WAND
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public SearchResult[] search(String query, double threshold) {

        // Lemmatisation : même traitement que SearchEngine
        String lemmatizedQuery = lemmatizer.lemmatizeQuery(query);
        if (lemmatizedQuery.isBlank()) return new SearchResult[0];

        IndexedPage queryPage = new IndexedPage(lemmatizedQuery);
        double queryLength = queryPage.getNorm();
        if (queryLength == 0) return new SearchResult[0];

        // Récupérer les listes de pages pour chaque mot de la requête
        List<PageList> lists           = new ArrayList<>();
        List<Double>   queryWordWeights = new ArrayList<>();

        for (String word : queryPage.getWords()) {
            PageList list = invertedIndex.get(word);
            if (list == null) continue;
            list.reset();
            lists.add(list);
            queryWordWeights.add(queryPage.getCount(word) / queryLength);
        }
        if (lists.isEmpty()) return new SearchResult[0];

        List<SearchResult> results = new ArrayList<>();

        while (true) {

            // Étape 1 : trier les listes par numéro de page actuel
            sortByCurrentPage(lists, queryWordWeights);
            if (lists.get(0).isExhausted()) break;

            // Étape 2 : trouver le "pivot" — le premier mot pour lequel
            // la somme des scores max possibles dépasse le seuil minimum
            int pivotIndex = findPivot(lists, queryWordWeights, threshold);
            if (pivotIndex == -1) break;

            int pivotPageNum = lists.get(pivotIndex).currentPage();

            // Étape 3 : aligner toutes les listes sur la page du pivot
            boolean allAligned = true;
            for (int i = 0; i < pivotIndex; i++) {
                if (lists.get(i).currentPage() < pivotPageNum) {
                    lists.get(i).advanceTo(pivotPageNum);
                    allAligned = false;
                }
            }

            // Étape 4 : si tout est aligné, calculer le vrai score de cette page
            if (allAligned) {
                double score = calculateScore(queryPage, pivotPageNum);
                if (score > threshold) {
                    String url = getPage(pivotPageNum).getUrl();
                    try { url = URLDecoder.decode(url, "UTF-8"); } catch (Exception ignored) {}
                    results.add(new SearchResult(url, score));
                }
                // Avancer toutes les listes au-delà de cette page
                for (int i = 0; i < lists.size(); i++) {
                    if (lists.get(i).currentPage() == pivotPageNum) {
                        lists.get(i).advanceTo(pivotPageNum + 1);
                    }
                }
            }
        }

        SearchResult[] table = results.toArray(new SearchResult[0]);
        Arrays.sort(table);
        return table;
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

    /**
     * Tri par insertion dichotomique : trie les listes par numéro de page actuel.
     * Utilise la dichotomie pour trouver rapidement la bonne position d'insertion,
     * puis décale les éléments des deux listes en parallèle.
     */
    private void sortByCurrentPage(List<PageList> lists, List<Double> weights) {
        for (int i = 1; i < lists.size(); i++) {
            PageList currentList = lists.get(i);
            double currentWeight = weights.get(i);
            int targetPage = currentList.currentPage();

            int lo = 0;
            int hi = i - 1;

            while (lo <= hi) {
                int mid = (lo + hi) / 2;
                if (lists.get(mid).currentPage() > targetPage) {
                    hi = mid - 1;
                } else {
                    lo = mid + 1;
                }
            }

            for (int j = i - 1; j >= lo; j--) {
                lists.set(j + 1, lists.get(j));
                weights.set(j + 1, weights.get(j));
            }

            lists.set(lo, currentList);
            weights.set(lo, currentWeight);
        }
    }

    /**
     * Cherche le pivot : on additionne les scores max possibles de chaque mot
     * jusqu'à dépasser le seuil. Le mot qui fait franchir le seuil est le pivot.
     * Si on arrive au bout sans dépasser le seuil, on retourne -1 (on peut s'arrêter).
     */
    private int findPivot(List<PageList> lists, List<Double> weights, double threshold) {
        double cumulative = 0.0;
        for (int i = 0; i < lists.size(); i++) {
            if (lists.get(i).isExhausted()) break;
            cumulative += weights.get(i) * lists.get(i).getMaxScorePossible();
            if (cumulative > threshold) return i;
        }
        return -1;
    }

    /**
     * Calcule le score de similarité réel en réutilisant directement 
     * la méthode de proximité de la classe parente.
     */
    private double calculateScore(IndexedPage queryPage, int pageNumber) {
        return queryPage.proximity(getPage(pageNumber));
    }
}