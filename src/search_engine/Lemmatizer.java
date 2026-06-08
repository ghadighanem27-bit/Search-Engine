package search_engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Lemmatizer {

    private final Map<String, String> dictionary;
    private final Set<String> blacklist;
    // Autocorrecteur initialisé avec les mots du dictionnaire
    private final Autocorrection autocorrection;

    public Lemmatizer(String dictionaryPath, String blacklistPath) throws IOException {
        this.dictionary = new HashMap<>();
        this.blacklist = new HashSet<>();
        loadDictionary(dictionaryPath);
        loadBlacklist(blacklistPath);
        // On initialise l'autocorrection avec tous les mots connus du dictionnaire
        this.autocorrection = new Autocorrection(dictionary.keySet());
    }

    private void loadDictionary(String dictionaryPath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(dictionaryPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) continue;
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    this.dictionary.put(parts[0], parts[1]);
                }
            }
        }
    }

    private void loadBlacklist(String blacklistPath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(blacklistPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) continue;
                this.blacklist.add(line);
            }
        }
    }

    public String lemmatizeQuery(String query) {
        if (query == null || query.length() == 0) return "";

        // Mise en minuscules + remplacement des caractères non alphabétiques par des espaces
        String cleanQuery = "";
        for (int i = 0; i < query.toLowerCase().length(); i++) {
            char c = query.toLowerCase().charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ') {
                cleanQuery += c;
            } else {
                cleanQuery += " ";
            }
        }

        String[] words = cleanQuery.split(" ");
        String result = "";

        for (String word : words) {
            // On ignore les mots de 2 lettres ou moins
            if (word.length() <= 2) continue;

            String lemma;

            if (this.dictionary.containsKey(word)) {
                // Le mot est dans le dictionnaire : on prend son lemme directement
                lemma = this.dictionary.get(word);
            } else {
                String correctedWord = autocorrection.correct(word);
                
                // On lemmatise le mot corrigé si possible
                if (this.dictionary.containsKey(correctedWord)) {
                    lemma = this.dictionary.get(correctedWord);
                } else {
                    lemma = correctedWord;
                }
            }

            // On ignore les mots de la liste noire
            if (this.blacklist.contains(lemma)) continue;

            if (result.length() == 0) {
                result = lemma;
            } else {
                result = result + " " + lemma;
            }
        }

        return result;
    }

    public int getDictionarySize() {
        return this.dictionary.size();
    }

    public Map<String, String> getFullDictionary() {
        return this.dictionary;
    }
}