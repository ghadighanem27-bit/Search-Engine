package search_engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Lemmatisation {

    private final Map<String, String> dictionary;
    private final Set<String> blacklist;

    public Lemmatisation(String dictionaryPath, String blacklistPath) throws IOException {
        this.dictionary = new HashMap<>();
        this.blacklist = new HashSet<>();
        loadDictionary(dictionaryPath);
        loadBlacklist(blacklistPath);
    }

    private void loadDictionary(String dictionaryPath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(dictionaryPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    String baseWord = parts[0];
                    String lemma = parts[1];
                    this.dictionary.put(baseWord, lemma);
                }
            }
        }
    }

    private void loadBlacklist(String blacklistPath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(blacklistPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                this.blacklist.add(line);
            }
        }
    }

    public String lemmatizeQuery(String query) {
        if (query == null || query.length() == 0) {
            return "";
        }

        String lowerQuery = query.toLowerCase();

        String cleanQuery = "";
        for (int i = 0; i < lowerQuery.length(); i++) {
            char c = lowerQuery.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ') {
                cleanQuery += c;
            } else {
                cleanQuery += " ";
            }
        }

        String[] words = cleanQuery.split(" ");
        String result = "";

        for (String word : words) {

            if (word.length() <= 2) {
                continue;
            }

            String lemma = word;
            if (this.dictionary.containsKey(word)) {
                lemma = this.dictionary.get(word);
            }

            if (this.blacklist.contains(lemma)) {
                continue;
            }

            if (result.length() == 0) {
                result = lemma;
            } else {
                result = result + " " + lemma;
            }
        }

        return result;
    }

    protected int getDictionarySize() {
        return this.dictionary.size();
    }

    public Map<String, String> getFullDictionary() {
        return this.dictionary;
    }
}