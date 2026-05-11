package search_engine;

import java.util.Arrays;

public class IndexedPage {
	private String url;
	private String[] words;
	private int[] counts;

	public IndexedPage(String[] lines) throws IllegalStateException {
		if (lines.length == 0) {
			throw new IllegalStateException("Le tableau est vide.");
		}
		this.url = lines[0];
		int n = lines.length - 1;
		this.words = new String[n];
		this.counts = new int[n];
		for (int i = 0; i < n; i++) {
			String[] separationColonne = lines[i + 1].split(":");
			this.words[i] = separationColonne[0];
			this.counts[i] = Integer.parseInt(separationColonne[1]);
		}
	}

	// public IndexedPage(Path path) {}
	
	
	public IndexedPage(String text) throws IllegalStateException {
		String[] motOrdre = text.toLowerCase().split("[^a-zA-Z]+");
		Arrays.sort(motOrdre);
		
		// si le premier element est vide, le texte ne contient aucun mot
	    if (motOrdre.length == 0 || motOrdre[0].equals("")) {
	        throw new IllegalStateException("Il n'y a pas de texte");
	    }
			
		this.words = new String[motOrdre.length];
		this.counts = new int[motOrdre.length];
		int j = -1;
		for (int i = 0; i < motOrdre.length; i++) {
			if (i == 0 || !motOrdre[i].equals(motOrdre[i - 1])) {
				j++;
				this.words[j] = motOrdre[i];
				this.counts[j] = 1;
			} else {
				this.counts[j]++;
			}
		}
		this.words = Arrays.copyOf(this.words, j + 1);
		this.counts = Arrays.copyOf(this.counts, j + 1);
	}

	public String getUrl() {
		return url;
	}

	public double getNorm() {
		double sum = 0;
		for (int count : counts) {
			sum += Math.pow(count, 2);
		}
		return Math.sqrt(sum);
	}

	private int getCount(String word) {
		for (int i = 0; i <  words.length; i++) {
			if (word.equals(words[i])) {
				return counts[i];
			}
		
		}
		return 0;
	}

	public double getPonderation(String word) {
		if (getNorm() == 0) {
			return 0;
		}
		return getCount(word) / getNorm();
	}

	public double proximity(IndexedPage page) {
		double sum = 0;

		for (int i = 0; i < this.words.length; i++) {
			String word = this.words[i];
			sum += this.getPonderation(word) * page.getPonderation(word);
		}
		return sum;
	}

	public String toString() {
		return "IndexedPage [url =" + getUrl() + "]";
	}

}
