package search_engine;


import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Scanner;

public class SearchEngine {

    private Path indexationDirectory;
    private IndexedPage[] pages;

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


    public IndexedPage getPage(int i) {
        return pages[i];
    }

    public int getPagesNumber() {
        return pages.length;
    }

    public SearchResult[] launchRequest(String requestString) {
        
        IndexedPage request = new IndexedPage(requestString);

        double[] scores = new double[pages.length];
        int resultCount = 0;
        for (int i = 0; i < pages.length; i++) {
            scores[i] = request.proximity(pages[i]);

            
            if (scores[i] > 1e-10) {
                resultCount++;
            }
        }

        
        SearchResult[] results = new SearchResult[resultCount];
        int resultIndex = 0;
        for (int i = 0; i < pages.length; i++) {
            if (scores[i] > 1e-10) {
                results[resultIndex] = new SearchResult(pages[i].getUrl(), scores[i]);
                resultIndex++;
            }
        }

        
        Arrays.sort(results);

        return results;
    }

    public void printResults(String requestString) {
        SearchResult[] results = launchRequest(requestString);

        
        int max = Math.min(15, results.length);
        for (int i = 0; i < max; i++) {
            System.out.println(results[i]);
        }
    }

    public static void main(String[] args) throws Exception {

        URL location = SearchEngine.class.getProtectionDomain().getCodeSource().getLocation();
        Path binFolder = Paths.get(location.toURI());
        Path indexFolder = binFolder.resolve("INDEX");

        SearchEngine se = new SearchEngine(indexFolder);

        if (args.length > 0) {
            
            String request = String.join(" ", args);
            se.printResults(request);
        } else {
           
            Scanner scanner = new Scanner(System.in);
            System.out.println("Bienvenue, tapez 'exit' pour quitter.");
            while (true) {
                System.out.print("Recherche : ");
                String line = scanner.nextLine().trim();
                if (line.equals("exit")) {
                    System.out.println("À bientôt !");
                    break;
                }
                if (!line.isEmpty()) {
                    se.printResults(line);
                }
            }
            scanner.close();
        }
    }
}
// Pour exécuter, se placer dans le dossier bin et lancer :
// java -cp . search_engine.SearchEngine cerise flan