package search_engine_tests;

import search_engine.*;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SearchEngineTests {

    public static void main(String[] args) {

        IndexedPage page1 = null;
        IndexedPage page2 = null;
        IndexedPage page3 = null;
        IndexedPage page4 = null;

        try {
            page1 = new IndexedPage(new String[] {"http://exemple.org/p1", "java:10", "code:5"});
        } catch (IllegalStateException e) {
            System.out.println("Erreur page1 : " + e.getMessage());
        }

        try {
            page2 = new IndexedPage(new String[] {"http://exemple.org/p2", "java:5", "test:2"});
        } catch (IllegalStateException e) {
            System.out.println("Erreur page2 : " + e.getMessage());
        }

        try {
            page3 = new IndexedPage("JAVA, java!   code... code");
        } catch (IllegalStateException e) {
            System.out.println("Erreur page3 : " + e.getMessage());
        }


        // --- Page 1 ---

        if (page1 != null) {
            System.out.println("--- Page 1 (constructeur par tableau) ---");
            System.out.println("  Représentation          : " + page1);
            System.out.println("  URL de la page          : " + page1.getUrl());
            System.out.println("  Poids total             : " + page1.getNorm());
            System.out.println("  Poids de 'java'         : " + page1.getPonderation("java"));
            System.out.println("  Poids de 'code'         : " + page1.getPonderation("code"));
            System.out.println("  Poids d'un mot absent   : " + page1.getPonderation("python"));
            System.out.println("  Similarité avec page1   : " + page1.proximity(page1)); // attendu 1.0
            if (page2 != null)
                System.out.println("  Similarité avec page2   : " + page1.proximity(page2));
            if (page3 != null)
                System.out.println("  Similarité avec page3   : " + page1.proximity(page3));
            System.out.println();
        }


        // --- Page 2 ---

        if (page2 != null) {
            System.out.println("--- Page 2 (constructeur par tableau) ---");
            System.out.println("  Représentation          : " + page2);
            System.out.println("  URL de la page          : " + page2.getUrl());
            System.out.println("  Poids total             : " + page2.getNorm());
            System.out.println("  Poids de 'java'         : " + page2.getPonderation("java"));
            System.out.println("  Poids de 'test'         : " + page2.getPonderation("test"));
            System.out.println("  Poids d'un mot absent   : " + page2.getPonderation("python"));
            System.out.println("  Similarité avec page2   : " + page2.proximity(page2)); // attendu 1.0
            if (page1 != null)
                System.out.println("  Similarité avec page1   : " + page2.proximity(page1));
            if (page3 != null)
                System.out.println("  Similarité avec page3   : " + page2.proximity(page3));
            System.out.println();
        }


        // --- Page 3 ---

        if (page3 != null) {
            System.out.println("--- Page 3 (constructeur par texte) ---");
            System.out.println("  Texte brut              : \"JAVA, java!   code... code\"");
            System.out.println("  Représentation          : " + page3);
            System.out.println("  URL de la page          : " + page3.getUrl());
            System.out.println("  Poids total             : " + page3.getNorm()); // attendu ~2.83
            System.out.println("  Poids de 'java'         : " + page3.getPonderation("java")); // attendu ~0.71
            System.out.println("  Poids de 'code'         : " + page3.getPonderation("code")); // attendu ~0.71
            System.out.println("  Poids d'un mot absent   : " + page3.getPonderation("python"));
            System.out.println("  Similarité avec page3   : " + page3.proximity(page3)); // attendu 1.0
            if (page1 != null)
                System.out.println("  Similarité avec page1   : " + page3.proximity(page1));
            if (page2 != null)
                System.out.println("  Similarité avec page2   : " + page3.proximity(page2));
            System.out.println();
        }


        // --- Page 4 ---

        System.out.println("--- Page 4 (constructeur par texte : texte vide) ---");
        System.out.println("  Texte brut              : \"   \"");
        try {
            page4 = new IndexedPage("   ");
        } catch (IllegalStateException e) {
            System.out.println("  Exception attendue      : " + e.getMessage());
        }
        System.out.println();

        // --- SearchEngine ---

        System.out.println("--- Test SearchEngine ---");
        try {
            URL location = SearchEngine.class.getProtectionDomain().getCodeSource().getLocation();
            Path binFolder = Paths.get(location.toURI());
            Path indexFolder = binFolder.resolve("INDEX");

            SearchEngine se = new SearchEngine(indexFolder);

            // On vérifie que le nombre de pages chargées est correct
            System.out.println("  Nombre de pages indexées : " + se.getPagesNumber());

            // On vérifie que getPage() retourne bien une page valide
            System.out.println("   page indexée    : " + se.getPage(0));

            // On lance une recherche et on affiche les résultats les plus pertinents
            System.out.println("  Résultats pour 'cerise flan' :");
            se.printResults("cerise flan");

        } catch (Exception e) {
            System.out.println("  Erreur SearchEngine : " + e.getMessage());
        }
        System.out.println();



        System.out.println("Fin des tests.");
    }
}

