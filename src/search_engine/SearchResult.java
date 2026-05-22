package search_engine;

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

	// On implémente Comparable pour définir l'ordre naturel de SearchResult.
	// Double.compare(b, a) au lieu de (a, b) permet d'inverser l'ordre naturel croissant
	// et permet d'éviter les erreurs de précision lors de la soustraction de plusieurs doubles
	@Override
	public int compareTo(SearchResult other) {
		return Double.compare(other.score, this.score);
	}

	@Override
	public String toString() {
		return "SearchResult [url=" + url + ", score=" + score + "]";
	}
}

