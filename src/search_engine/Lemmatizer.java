package search_engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transforme les mots d'une requête en leur forme canonique (lemme).
 * Utilise un dictionnaire de lemmatisation et une liste noire de mots à exclure.
 * Tous les mots sont normalisés via IndexedPage.normalize() pour garantir
 * la cohérence avec les mots stockés dans les fichiers d'index.
 */
public class Lemmatizer {

    private final Map<String, String> dictionary;
    private final Set<String> blacklist;

    /**
     * Charge le dictionnaire et la liste noire depuis les fichiers indiqués.
     */
    public Lemmatizer(String dictionaryPath, String blacklistPath) throws IOException {
        this.dictionary = new HashMap<>();
        this.blacklist  = new HashSet<>();
        loadDictionary(dictionaryPath);
        loadBlacklist(blacklistPath);
    }

    /**
     * Charge le dictionnaire de lemmatisation depuis un fichier texte.
     * Chaque ligne a le format "forme:lemme".
     * Les clés et valeurs sont normalisées pour correspondre au format des index.
     */
    private void loadDictionary(String path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key   = IndexedPage.normalize(parts[0]);
                    String lemma = IndexedPage.normalize(parts[1]);
                    this.dictionary.put(key, lemma);
                }
            }
        }
    }

    /**
     * Charge la liste noire des mots à ignorer lors d'une recherche.
     * Les mots sont normalisés pour correspondre au format des index.
     */
    private void loadBlacklist(String path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String normalized = IndexedPage.normalize(line.trim());
                if (!normalized.isEmpty()) this.blacklist.add(normalized);
            }
        }
    }

    /**
     * Transforme une requête brute en une suite de lemmes normalisés.
     * Les mots de moins de 3 caractères et ceux présents dans la liste noire sont ignorés.
     * Chaque mot est d'abord normalisé, puis remplacé par son lemme si disponible.
     */
    public String lemmatizeQuery(String query) {
        if (query == null || query.isBlank()) return "";

        // On extrait uniquement les lettres (y compris accentuées) pour nettoyer la ponctuation
        String cleaned = query.toLowerCase().replaceAll("\\P{L}+", " ").trim();
        String[] tokens = cleaned.split("\\s+");

        StringBuilder result = new StringBuilder();
        for (String token : tokens) {
            // On ignore les mots trop courts, souvent non significatifs
            if (token.length() <= 2) continue;

            // Normalisation du mot pour être cohérent avec les clés du dictionnaire
            String normalized = IndexedPage.normalize(token);
            if (normalized.isEmpty()) continue;

            // Remplacement par le lemme si présent dans le dictionnaire
            String lemma = dictionary.getOrDefault(normalized, normalized);

            // Exclusion des mots présents dans la liste noire
            if (blacklist.contains(lemma)) continue;

            if (result.length() > 0) result.append(" ");
            result.append(lemma);
        }

        return result.toString();
    }
}