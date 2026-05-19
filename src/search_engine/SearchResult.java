//voir les fonctions de la SAE

package search_engine_tests;

public class SearchResult implements Comparable<SearchResult> {
	private String url;
	private double score;

	public SearchResult(String url, double score) {
		this.url = url;
		this.score = score;
	}

	public String getUrl() {
		return url;
	}

	public double getScore() {
		return score;
	}

	// Tri décroissant : le plus pertinent en premier
	@Override
	public int compareTo(SearchResult other) {
		return Double.compare(other.score, this.score);
	}

	@Override
	public String toString() {
		return "SearchResult [url=" + url + ", score=" + score + "]";
	}
}
