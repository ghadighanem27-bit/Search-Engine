package search_engine_tests;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import search_engine.IndexedPage;
import search_engine.SearchEngine;
import search_engine.SearchResult;

public class SearchEngineTests {

    public static void main(String[] args) {

        // --- Test 1 : Normalisation des mots ---
        String t1 = IndexedPage.normalize("Java");
        String t2 = IndexedPage.normalize("Météo");
        String t3 = IndexedPage.normalize("anti-constitutionnellement !");
        
        System.out.println("--- Test 1 : Normalisation ---");
        System.out.println("  'Java' -> '" + t1 + "'");
        System.out.println("  'Météo' -> '" + t2 + "'");
        System.out.println("  'anti-constitutionnellement !' -> '" + t3 + "'");
        
        if (t1.equals("java") && t2.equals("meteo") && t3.equals("anticonstitutionnellement")) {
            System.out.println("Erreur page1 : OK");
        } else {
            System.out.println("Erreur page1 : Erreur de normalisation");
        }
        System.out.println();

        // --- Test 2 : Page virtuelle (Texte brut) ---
        IndexedPage queryPage = new IndexedPage("java code java");
        int countJava = queryPage.getCount("java");
        int countCode = queryPage.getCount("code");
        double queryNorm = queryPage.getNorm();
        
        System.out.println("--- Test 2 : Page virtuelle ---");
        System.out.println("  java : " + countJava);
        System.out.println("  code : " + countCode);
        System.out.println("  norme : " + queryNorm);
        System.out.println();

        // --- Test 3 : Page indexée (Fichier) ---
        IndexedPage filePage = null;
        System.out.println("--- Test 3 : Page indexee ---");
        try {
            Path tempFile = Files.createTempFile("page_test_", ".txt");
            Files.write(tempFile, List.of("http://mon-url-de-test.com/index.html", "Java:4", "Code:3"));
            
            filePage = new IndexedPage(tempFile);
            
            System.out.println("  url : " + filePage.getUrl());
            System.out.println("  java : " + filePage.getCount("java"));
            System.out.println("  code : " + filePage.getCount("code"));
            System.out.println("  norme : " + filePage.getNorm());
            
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            System.out.println("Erreur page1 : " + e.getMessage());
        }
        System.out.println();

        // --- Test 4 : Similarité Cosinus ---
        System.out.println("--- Test 4 : Proximite ---");
        if (queryPage != null && filePage != null) {
            double sim = queryPage.proximity(filePage);
            System.out.println("  proximite : " + sim);
        } else {
            System.out.println("Erreur page1 : Pages non initialisees");
        }
        System.out.println();

        // --- Test 5 : Moteur de Recherche ---
        System.out.println("--- Test 5 : SearchEngine ---");
        try {
            URL location = SearchEngine.class.getProtectionDomain().getCodeSource().getLocation();
            Path binFolder = Paths.get(location.toURI());
            Path indexFolder = binFolder.getParent().resolve(Paths.get("doc", "INDEX"));
            Path lemmasFolder = binFolder.getParent().resolve(Paths.get("doc", "LEMMES"));

            SearchEngine engine = new SearchEngine(indexFolder, lemmasFolder);
            System.out.println("  Pages chargees : " + engine.getPagesNumber());

            SearchResult[] results = engine.search("java");
            for (int i = 0; i < Math.min(5, results.length); i++) {
                System.out.println("  " + results[i]);
            }
        } catch (Exception e) {
            System.out.println("Erreur SearchEngine : " + e.getMessage());
        }
    }
}