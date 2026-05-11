package search_engine_tests;

import search_engine.*;

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
            System.out.println("  Representation          : " + page1);
            System.out.println("  URL de la page          : " + page1.getUrl());
            System.out.println("  Poids total             : " + page1.getNorm());
            System.out.println("  Poids de 'java'         : " + page1.getPonderation("java"));
            System.out.println("  Poids de 'code'         : " + page1.getPonderation("code"));
            System.out.println("  Poids d'un mot absent   : " + page1.getPonderation("python"));
            System.out.println("  Similarite avec page1   : " + page1.proximity(page1)); // attendu 1.0
            if (page2 != null)
                System.out.println("  Similarite avec page2   : " + page1.proximity(page2));
            if (page3 != null)
                System.out.println("  Similarite avec page3   : " + page1.proximity(page3));
            System.out.println();
        }


        // --- Page 2 ---

        if (page2 != null) {
            System.out.println("--- Page 2 (constructeur par tableau) ---");
            System.out.println("  Representation          : " + page2);
            System.out.println("  URL de la page          : " + page2.getUrl());
            System.out.println("  Poids total             : " + page2.getNorm());
            System.out.println("  Poids de 'java'         : " + page2.getPonderation("java"));
            System.out.println("  Poids de 'test'         : " + page2.getPonderation("test"));
            System.out.println("  Poids d'un mot absent   : " + page2.getPonderation("python"));
            System.out.println("  Similarite avec page2   : " + page2.proximity(page2)); // attendu 1.0
            if (page1 != null)
                System.out.println("  Similarite avec page1   : " + page2.proximity(page1));
            if (page3 != null)
                System.out.println("  Similarite avec page3   : " + page2.proximity(page3));
            System.out.println();
        }


        // --- Page 3 ---

        if (page3 != null) {
            System.out.println("--- Page 3 (constructeur par texte) ---");
            System.out.println("  Texte brut              : \"JAVA, java!   code... code\"");
            System.out.println("  Representation          : " + page3);
            System.out.println("  URL de la page          : " + page3.getUrl());
            System.out.println("  Poids total             : " + page3.getNorm()); // attendu ~2.83
            System.out.println("  Poids de 'java'         : " + page3.getPonderation("java")); // attendu ~0.71
            System.out.println("  Poids de 'code'         : " + page3.getPonderation("code")); // attendu ~0.71
            System.out.println("  Poids d'un mot absent   : " + page3.getPonderation("python"));
            System.out.println("  Similarite avec page3   : " + page3.proximity(page3)); // attendu 1.0
            if (page1 != null)
                System.out.println("  Similarite avec page1   : " + page3.proximity(page1));
            if (page2 != null)
                System.out.println("  Similarite avec page2   : " + page3.proximity(page2));
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


        System.out.println("Fin des tests.");
    }
}