package search_engine;

import java.util.HashSet;
import java.util.Set;

public class Autocorrection {

    // Seuil maximum de distance de Levenshtein pour accepter une correction
    private static final int LEVENSHTEIN_THRESHOLD = 2;

    // Seuil maximum de distance de Jaccard pour la présélection
    private static final double JACCARD_THRESHOLD = 0.8;

    // Le dictionnaire de lemmes (mots connus)
    private final String[] dictionaryWords;

    public Autocorrection(Set<String> dictionary) {
        // On stocke tous les mots du dictionnaire dans un tableau
        this.dictionaryWords = dictionary.toArray(new String[0]);
    }

    /**
     * Tente de corriger un mot non reconnu.
     * Retourne le mot corrigé, ou le mot original si aucune correction n'est trouvée.
     */
    public String correct(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }

        String bestCandidate = null;
        int bestDistance = Integer.MAX_VALUE;

        // On représente le mot à corriger comme un ensemble de bigrammes
        Set<String> wordBigrams = getBigrams(word);

        for (String candidate : dictionaryWords) {
            // Phase 1 : présélection rapide par distance de Jaccard
            Set<String> candidateBigrams = getBigrams(candidate);
            double jaccardDist = jaccardDistance(wordBigrams, candidateBigrams);

            if (jaccardDist > JACCARD_THRESHOLD) {
                continue; // trop différent, on passe
            }

            // Phase 2 : calcul précis de la distance de Levenshtein
            int levenshteinDist = levenshteinDistance(word, candidate);

            if (levenshteinDist < bestDistance) {
                bestDistance = levenshteinDist;
                bestCandidate = candidate;
            }
        }

        // On ne corrige que si la distance est dans le seuil accepté
        if (bestCandidate != null && bestDistance <= LEVENSHTEIN_THRESHOLD) {
            return bestCandidate;
        }

        // Aucune correction trouvée : on retourne le mot original
        return word;
    }

    /**
     * Calcule la distance de Damerau-Levenshtein entre deux mots.
     * Gère les insertions, suppressions, substitutions ET transpositions (inversion de lettres).
     * Algorithme de Wagner-Fischer (programmation dynamique).
     */
    public static int levenshteinDistance(String word1, String word2) {
        int len1 = word1.length();
        int len2 = word2.length();

        int[][] distanceMatrix = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) distanceMatrix[i][0] = i;
        for (int j = 0; j <= len2; j++) distanceMatrix[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int substitutionCost = (word1.charAt(i - 1) == word2.charAt(j - 1)) ? 0 : 1;
                
                distanceMatrix[i][j] = minimum(
                    distanceMatrix[i - 1][j] + 1,                    // suppression
                    distanceMatrix[i][j - 1] + 1,                    // insertion
                    distanceMatrix[i - 1][j - 1] + substitutionCost  // substitution
                );

                // Ajout Damerau : gestion de la transposition (inversion de 2 lettres adjacentes)
                if (i > 1 && j > 1 && 
                    word1.charAt(i - 1) == word2.charAt(j - 2) && 
                    word1.charAt(i - 2) == word2.charAt(j - 1)) {
                    
                    distanceMatrix[i][j] = Math.min(distanceMatrix[i][j], distanceMatrix[i - 2][j - 2] + substitutionCost);
                }
            }
        }

        return distanceMatrix[len1][len2];
    }

    /**
     * Calcule la distance de Jaccard entre deux ensembles de bigrammes.
     * distance = 1 - (|A ∩ B| / |A ∪ B|)
     * Retourne une valeur entre 0 (identiques) et 1 (totalement différents).
     */
    public static double jaccardDistance(Set<String> setA, Set<String> setB) {
        if (setA.isEmpty() && setB.isEmpty()) return 0.0;

        // Calcul de l'intersection
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        // Calcul de l'union
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return 1.0 - ((double) intersection.size() / union.size());
    }

    /**
     * Génère l'ensemble des bigrammes d'un mot.
     * Ex: "cheval" → {"ch", "he", "ev", "va", "al"}
     */
    public static Set<String> getBigrams(String word) {
        Set<String> bigrams = new HashSet<>();
        
        // Sécurité pour les mots trop courts
        if (word == null || word.length() < 2) {
            return bigrams;
        }
        
        // On s'arrête à length() - 1 pour prendre des blocs de 2
        for (int i = 0; i < word.length() - 1; i++) {
            bigrams.add(word.substring(i, i + 2)); // bigrammes de 2 lettres
        }
        return bigrams;
    }

    /**
     * Retourne le minimum de trois entiers.
     */
    private static int minimum(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }
}