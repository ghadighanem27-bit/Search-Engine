package search_engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente une page indexée sous forme de vecteur de mots.
 * Chaque mot est associé à son nombre d'occurrences dans la page.
 * La similarité entre deux pages est calculée par produit scalaire normalisé (cosinus).
 */
public class IndexedPage {

    private String url;
    private String[] words;
    private int[] counts;
    private Map<String, Integer> countByWord;
    private double norm;

    /**
     * Normalise un mot : minuscules, suppression des accents et des caractères spéciaux.
     * Utilisé uniformément à l'indexation et à la recherche pour garantir la cohérence.
     */
    public static String normalize(String s) {
        if (s == null) return "";
        String str = s.toLowerCase().trim();
        str = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        str = str.replaceAll("\\p{M}", "");
        return str.replaceAll("[^a-z0-9]", "");
    }

    /**
     * Construit une page indexée à partir d'un fichier.
     * La première ligne contient l'URL, les suivantes ont le format "mot:occurrences".
     * Le fichier est lu en UTF-8, avec repli sur Latin-1 en cas d'erreur d'encodage.
     */
    public IndexedPage(Path path) throws IOException {
        List<String> lines;
        
        lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);

        if (lines.isEmpty()) throw new IllegalArgumentException("Fichier vide : " + path);

        this.url = lines.get(0);
        int n = lines.size() - 1;
        this.words  = new String[n];
        this.counts = new int[n];

        for (int i = 0; i < n; i++) {
            String line = lines.get(i + 1);
            if (!line.contains(":")) continue;
            String[] parts = line.split(":", 2);
            // Chaque mot est normalisé pour être comparable à la requête
            this.words[i] = normalize(parts[0]);
            try {
                this.counts[i] = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                this.counts[i] = 0;
            }
        }
        buildVectorData();
    }

    /**
     * Construit une page virtuelle à partir d'un texte brut (utilisé pour la requête).
     * Chaque mot unique du texte devient une entrée du vecteur avec son nombre d'occurrences.
     */
    public IndexedPage(String text) {
        String[] rawWords = text.split("\\s+");
        Map<String, Integer> tempMap = new HashMap<>();

        for (String w : rawWords) {
            String normalized = normalize(w);
            if (!normalized.isEmpty()) {
                tempMap.put(normalized, tempMap.getOrDefault(normalized, 0) + 1);
            }
        }

        this.words  = tempMap.keySet().toArray(new String[0]);
        this.counts = new int[this.words.length];
        for (int i = 0; i < this.words.length; i++) {
            this.counts[i] = tempMap.get(this.words[i]);
        }
        buildVectorData();
    }

    /**
     * Construit la map mot→occurrences et calcule la norme du vecteur.
     * La norme est la racine carrée de la somme des carrés des occurrences.
     */
    private void buildVectorData() {
        this.countByWord = new HashMap<>();
        double sumOfSquares = 0;
        for (int i = 0; i < this.words.length; i++) {
            this.countByWord.put(this.words[i], this.counts[i]);
            sumOfSquares += (double) this.counts[i] * this.counts[i];
        }
        this.norm = Math.sqrt(sumOfSquares);
    }

    public String getUrl()               { return url; }
    public double getNorm()              { return norm; }
    public Iterable<String> getWords()   { return countByWord.keySet(); }
    public int getCount(String word)     { return countByWord.getOrDefault(word, 0); }

    /**
     * Calcule la similarité cosinus entre cette page et une autre.
     * Retourne une valeur entre 0 (aucune similarité) et 1 (identiques).
     */
    public double proximity(IndexedPage other) {
        if (this.norm == 0 || other.norm == 0) return 0;
        double dotProduct = 0;
        for (int i = 0; i < this.words.length; i++) {
            int countInOther = other.getCount(this.words[i]);
            dotProduct += ((double) this.counts[i] / this.norm) * ((double) countInOther / other.norm);
        }
        return dotProduct;
    }
}