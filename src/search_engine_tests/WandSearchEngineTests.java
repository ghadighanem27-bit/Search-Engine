package search_engine_tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import search_engine.IndexedPage;
import search_engine.SearchEngine;
import search_engine.SearchResult;
import search_engine.WandSearchEngine;

/**
 * Tests de WandSearchEngine.
 * On ne peut plus tester PostingList et InvertedIndex directement car
 * ils sont privés dans WandSearchEngine. On teste donc le comportement
 * global : WAND doit retourner exactement les mêmes résultats que le
 * moteur brute-force, dans le même ordre.
 */
public class WandSearchEngineTests {

    private static int passed = 0;
    private static int failed = 0;

    private static void ok(String name)                  { System.out.println("   [OK]   " + name); passed++; }
    private static void fail(String name, String reason) { System.out.println("   [FAIL] " + name + " — " + reason); failed++; }
    private static void assertTrue(String name, boolean condition) { if (condition) ok(name); else fail(name, "condition false"); }

    // Crée une IndexedPage depuis un fichier temporaire (url + mot:count)
    private static IndexedPage makePage(String url, String... termCounts) throws IOException {
        Path tmp = Files.createTempFile("wand_", ".txt");
        List<String> lines = new ArrayList<>();
        lines.add(url);
        for (String tc : termCounts) lines.add(tc);
        Files.write(tmp, lines);
        IndexedPage page = new IndexedPage(tmp);
        Files.deleteIfExists(tmp);
        return page;
    }

    // Crée un dossier temporaire contenant des fichiers de pages
    private static Path makeTempIndex(IndexedPage... pages) throws IOException {
        Path dir = Files.createTempDirectory("wand_index_");
        for (int i = 0; i < pages.length; i++) {
            Path file = dir.resolve(i + ".txt");
            List<String> lines = new ArrayList<>();
            lines.add(pages[i].getUrl());
            for (String word : pages[i].getWords()) {
                lines.add(word + ":" + pages[i].getCount(word));
            }
            Files.write(file, lines);
        }
        return dir;
    }

    // Crée un dossier LEMMES minimal (dico vide + blacklist vide)
    private static Path makeTempLemmas() throws IOException {
        Path dir = Files.createTempDirectory("wand_lemmas_");
        Files.write(dir.resolve("dico.txt"), new ArrayList<>());
        Files.write(dir.resolve("blacklist.txt"), new ArrayList<>());
        return dir;
    }

    // Vérifie que deux tableaux de résultats sont identiques
    private static boolean sameResults(SearchResult[] a, SearchResult[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (!a[i].getUrl().equals(b[i].getUrl())) return false;
            if (Math.abs(a[i].getScore() - b[i].getScore()) > 1e-9) return false;
        }
        return true;
    }

    // ── Test 1 : requête simple ───────────────────────────────────────────────
    static void testSimpleQuery() throws IOException {
        System.out.println("\n--- Test 1 : requête simple (1 mot) ---");

        IndexedPage p0 = makePage("http://a.com", "chat:4", "noir:2");
        IndexedPage p1 = makePage("http://b.com", "chat:1", "velours:3");
        IndexedPage p2 = makePage("http://c.com", "noir:6");

        Path index  = makeTempIndex(p0, p1, p2);
        Path lemmas = makeTempLemmas();

        SearchEngine brute = new SearchEngine(index, lemmas);
        SearchEngine wand  = new WandSearchEngine(index, lemmas);

        SearchResult[] rb = brute.search("chat");
        SearchResult[] rw = wand.search("chat");

        assertTrue("mêmes résultats pour 'chat'", sameResults(rb, rw));
        assertTrue("au moins 1 résultat", rw.length > 0);
    }

    // ── Test 2 : requête multi-mots ───────────────────────────────────────────
    static void testMultiWordsQuery() throws IOException {
        System.out.println("\n--- Test 2 : requête multi-mots ---");

        IndexedPage p0 = makePage("http://a.com", "chat:4", "noir:2");
        IndexedPage p1 = makePage("http://b.com", "chat:1", "velours:3");
        IndexedPage p2 = makePage("http://c.com", "noir:6");
        IndexedPage p3 = makePage("http://d.com", "chat:2", "noir:3", "velours:1");

        Path index  = makeTempIndex(p0, p1, p2, p3);
        Path lemmas = makeTempLemmas();

        SearchEngine brute = new SearchEngine(index, lemmas);
        SearchEngine wand  = new WandSearchEngine(index, lemmas);

        for (String query : new String[]{"chat noir", "chat velours", "chat noir velours"}) {
            SearchResult[] rb = brute.search(query);
            SearchResult[] rw = wand.search(query);
            assertTrue("mêmes résultats pour '" + query + "'", sameResults(rb, rw));
        }
    }

    // ── Test 3 : mot absent de l'index ────────────────────────────────────────
    static void testMissingWord() throws IOException {
        System.out.println("\n--- Test 3 : mot absent de l'index ---");

        IndexedPage p0 = makePage("http://a.com", "chat:3");
        Path index  = makeTempIndex(p0);
        Path lemmas = makeTempLemmas();

        SearchEngine wand = new WandSearchEngine(index, lemmas);
        SearchResult[] r  = wand.search("inexistant");

        assertTrue("0 résultat pour un mot absent", r.length == 0);
        ok("pas d'exception");
    }

    // ── Test 4 : requête vide ─────────────────────────────────────────────────
    static void testEmptyQuery() throws IOException {
        System.out.println("\n--- Test 4 : requête vide ---");

        IndexedPage p0 = makePage("http://a.com", "chat:3");
        Path index  = makeTempIndex(p0);
        Path lemmas = makeTempLemmas();

        SearchEngine wand = new WandSearchEngine(index, lemmas);
        SearchResult[] r  = wand.search("");

        assertTrue("0 résultat pour une requête vide", r.length == 0);
        ok("pas d'exception");
    }

    // ── Test 5 : résultats triés par score décroissant ────────────────────────
    static void testSortByScore() throws IOException {
        System.out.println("\n--- Test 5 : résultats triés par score ---");

        IndexedPage p0 = makePage("http://a.com", "chat:1");
        IndexedPage p1 = makePage("http://b.com", "chat:5");
        IndexedPage p2 = makePage("http://c.com", "chat:3");

        Path index  = makeTempIndex(p0, p1, p2);
        Path lemmas = makeTempLemmas();

        SearchEngine wand = new WandSearchEngine(index, lemmas);
        SearchResult[] r  = wand.search("chat");

        boolean wellSorted = true;
        for (int i = 0; i < r.length - 1; i++) {
            if (r[i].getScore() < r[i + 1].getScore()) { wellSorted = false; break; }
        }
        assertTrue("résultats triés par score décroissant", wellSorted);
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        System.out.println("=== Tests WandSearchEngine ===");
        testSimpleQuery();
        testMultiWordsQuery();
        testMissingWord();
        testEmptyQuery();
        testSortByScore();
        System.out.println("\n=== " + passed + " OK, " + failed + " FAIL ===");
    }
}