package search_engine;
import java.util.List;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class IndexedPage {
	private String url;
	private String[] words;
	private int[] counts;
	private Map<String, Integer> countByWord;
	private double norm;

	public IndexedPage(String[] lines) throws IllegalStateException {
		if (lines.length == 0) {
			throw new IllegalStateException("Le tableau est vide.");
		}
		this.url = lines[0];
		int n = lines.length - 1;
		this.words = new String[n];
		this.counts = new int[n];
		for (int i = 0; i < n; i++) {
			String[] parts = lines[i + 1].split(":",2);
			this.words[i] = parts[0];
			this.counts[i] = Integer.parseInt(parts[1]);
		}
		buildDerivedData();
	}

	 public IndexedPage(Path path) throws IOException {
		List<String> lines = Files.readAllLines(path);

		if (lines.isEmpty()) {
			throw new IllegalArgumentException("Le fichier mis en parametre est vide");
		}
		this.url = lines.get(0);
		int totalWords = lines.size() - 1;
		this.words = new String[totalWords];
		this.counts = new int[totalWords];
		for (int i = 0; i < totalWords; i++) {
			String[] parts = lines.get(i + 1).split(":",2);
			this.words[i] = parts[0];
			this.counts[i] = Integer.parseInt(parts[1]);
		}
		buildDerivedData();
	 }
	
	
	public IndexedPage(String text) throws IllegalStateException {
		String[] splitWords = text.toLowerCase().split("[^a-zA-Z]+");
		
		// Compter les mots non vides
		int wordCount = 0;
		for (String m : splitWords) if (!m.isEmpty()) wordCount++;
		
		if (wordCount == 0) throw new IllegalStateException("Il n'y a pas de texte");
		
		String[] sortedWords = new String[wordCount];
		int writeIndex = 0;
		for (String m : splitWords) if (!m.isEmpty()) sortedWords[writeIndex++] = m;
		
		Arrays.sort(sortedWords);
		
		this.words = new String[sortedWords.length];
		this.counts = new int[sortedWords.length];
		int uniqueIndex = -1;
		for (int i = 0; i < sortedWords.length; i++) {
			if (i == 0 || !sortedWords[i].equals(sortedWords[i - 1])) {
				uniqueIndex++;
				this.words[uniqueIndex] = sortedWords[i];
				this.counts[uniqueIndex] = 1;
			} else {
				this.counts[uniqueIndex]++;
			}
		}
		this.words = Arrays.copyOf(this.words, uniqueIndex + 1);
		this.counts = Arrays.copyOf(this.counts, uniqueIndex + 1);
		buildDerivedData();
	}

	private void buildDerivedData() {
		this.countByWord = new HashMap<>();
		double sum = 0;
		for (int i = 0; i < this.words.length; i++) {
			this.countByWord.put(this.words[i], this.counts[i]);
			sum += (double) this.counts[i] * this.counts[i];
		}
		this.norm = Math.sqrt(sum);
	}

	public String getUrl() {
		return url;
	}

	public double getNorm() {
		return norm;
	}

	public int getCount(String word) {
		return countByWord.getOrDefault(word, 0);
	}

	public double getPonderation(String word) {
		if (getNorm() == 0) {
			return 0;
		}
		return getCount(word) / getNorm();
	}

	public double proximity(IndexedPage page) {
		if (this.norm == 0 || page.norm == 0) {
			return 0;
		}
		double sum = 0;

		for (int i = 0; i < this.words.length; i++) {
			int countInPage = page.getCount(this.words[i]);
			if (countInPage == 0) {
				continue;
			}
			sum += ((double) this.counts[i] / this.norm) * ((double) countInPage / page.norm);
		}
		return sum;
	}

	public String toString() {
		return "IndexedPage [url=" + getUrl() + "]";
	}

}


