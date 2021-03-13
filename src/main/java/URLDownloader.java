
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.print.Doc;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;


//TODO: change ALL strings to constants

public class URLDownloader {
    private static final int NUM_OF_ARGS = 4;
    private static final String IO_ERROR_MSG = "Please supply:" + "\n"
            + "1. URL to scrap" + "\n"
            + "2. Maximal number of URLs to fetch" + "\n"
            + "3. Search depth factor" + "\n"
            + "4. 1 for cross-level uniqueness, 0 otherwise";
    private static final String invalidCharRegex = "[,.<>:\"/|\\?*]";
    private static final ArrayList<Character> invalidChars =
            new ArrayList<>(Arrays.asList(',', '.', '<', '>', ':', '"', '/', '|', '\\', '?', '*'));

    private HashSet<String> fileNames = new HashSet<>();
    private static int maxUrls;
    private static int unique;
    private static int depthFactor;

    public static void main(String[] args) throws IOException {
        //TODO: multi threading, ForkJoinPool?, Thread number Threshold!
        Validate.isTrue(args.length == NUM_OF_ARGS, IO_ERROR_MSG);
        String url = args[0];
        maxUrls = Integer.parseInt(args[1]);
        depthFactor = Integer.parseInt(args[2]);
        unique = Integer.parseInt(args[3]);
        //TODO: verify validity of parameters
        print("Fetching %s...", url);

        Document doc = Jsoup.connect(url).get();
        //TODO: save to file
        createDirectories(depthFactor);

        urlToHtml(doc, url);

        Elements links = doc.select("a[href]");
        print("\nLinks: (%d)", links.size());
        for (Element link : links) {
            print(" * a: <%s>  (%s)", link.attr("abs:href"), trim(link.text(), 35));
        }
    }

    private static void createDirectories(int depthFactor) {
        for (int i = 0; i <= depthFactor; i++) {
            try {
                Path path = Paths.get(String.valueOf(i));
                Files.createDirectory(path);
            } catch (IOException e) {
                System.out.println("Error creating directory");
                e.printStackTrace();
            }
        }
    }


    private static void urlToHtml(Document doc, String fileName) {
        try {
            //TODO: remove https://
            File newFile = new File(fileName);
            if (newFile.createNewFile()) {
                System.out.println("File " + fileName + " created");
                writeToFile(doc, fileName);
            } else {
                System.out.println("File already exists");
            }
        } catch (IOException e) {

            System.out.println("Error creating file");
            e.printStackTrace();
        }
    }

    private static void writeToFile(Document doc, String fileName) {
        try {
            FileWriter myWriter = new FileWriter(fileName);
            myWriter.write(doc.html());
            myWriter.close();
            System.out.println("Wrote to file " + fileName);
        } catch (IOException e) {
            System.out.println("Error writing file");
            e.printStackTrace();
        }
    }

    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

    private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width - 1) + ".";
        else
            return s;
    }

    private class URLTask implements Runnable {
        private Thread _myThread;
        private String _url;
        private int _level;
        private AtomicInteger _filesCreatedByParent;
        private AtomicInteger _filesCreated;


        public URLTask(URLDownloader downloader, String url, int level, AtomicInteger filesCreatedByParent) {
            _url = url;
            _level = level;
            _filesCreatedByParent = filesCreatedByParent;
            _filesCreated = new AtomicInteger(0);
        }

        @Override
        public void run() {
            //add self
            try{

                String fileName = _url.replaceAll("https://", "").replaceAll(invalidCharRegex, "_")
                        + ".html";
                Document doc = Jsoup.connect(_url).get();
                if (unique == 0 || !fileNames.contains(fileName)) {
                    urlToHtml(doc, _url);
                    _filesCreatedByParent.getAndIncrement();
                }
            }
            catch (IOException e){
                //TODO: something
            }

            //TODO: handle children, should be in thread
//            if (_level <= depthFactor) {
//                Elements links = doc.select("a[href]");
//                int pagesToLoad = Math.min(links.size(),maxUrls);
//                int i = 0;
//                while (i<pagesToLoad)
//                {
//
//                }
//                for (Element link : links) {
//                    print(" * a: <%s>  (%s)", link.attr("abs:href"), trim(link.text(), 35));
//                }
//            }

        }

    }

}