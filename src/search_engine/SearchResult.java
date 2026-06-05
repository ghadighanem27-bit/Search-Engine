package search_engine;

/**
 * Représente un résultat de recherche associant une URL à son score de pertinence.
 * Les résultats sont triables par ordre décroissant de score.
 */
public class SearchResult implements Comparable<SearchResult> {

    private String url;
    private double score;

    public SearchResult(String url, double score) {
        this.url   = url;
        this.score = score;
    }

    public String getUrl()   { return url; }
    public double getScore() { return score; }

    /**
     * Tri par score décroissant : le résultat le plus pertinent apparaît en premier.
     */
    @Override
    public int compareTo(SearchResult other) {
        return Double.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return url + " — score : " + score;
    }
}