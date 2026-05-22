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

	@Override
	public int compareTo(SearchResult other) {
		return Double.compare(other.score, this.score);
	}

	@Override
	public String toString() {
		return "SearchResult [url=" + url + ", score=" + score + "]";
	}
}