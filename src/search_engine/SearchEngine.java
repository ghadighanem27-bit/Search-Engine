package search_engine;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchEngine {

    private IndexedPage[] pages;
    private Path indexationDirectory;

    public SearchEngine(Path indexationDirectory) throws IOException {
        this.indexationDirectory = indexationDirectory;
        List<IndexedPage> list = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(indexationDirectory, "*.txt")) {
            for (Path file : stream) {
                list.add(new IndexedPage(file));
            }
        }
        this.pages = list.toArray(new IndexedPage[0]);
    }

    public List<SearchResult> getResults(String requestString) {
        IndexedPage request = new IndexedPage(requestString);
        List<SearchResult> results = new ArrayList<>();

        for (IndexedPage page : pages) {
            double score = request.proximity(page);
            if (score > 0) {
                results.add(new SearchResult(page.getUrl(), score));
            }
        }

        Collections.sort(results);
        return results;
    }

    public IndexedPage getPage(int i) {
        if (i >= 0 && i < pages.length) {
            return pages[i];
        }
        throw new IndexOutOfBoundsException("L'index " + i + " est hors limites.");
    }

    public int getPagesNumber() {
        return pages.length;
    }

    public SearchResult[] launchRequest(String requestString) {
        List<SearchResult> results = getResults(requestString);
        return results.toArray(new SearchResult[0]);
    }

    public void printResults(String requestString) {
        List<SearchResult> results = getResults(requestString);
        int max = Math.min(15, results.size());
        for (int i = 0; i < max; i++) {
            System.out.println(results.get(i));
        }
    }
}