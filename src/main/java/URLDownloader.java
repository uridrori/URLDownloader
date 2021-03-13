
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
//TODO: documentation

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

    private static final HashSet<String> fileNames = new HashSet<>();
    private static int maxUrls;
    private static int unique;
    private static int depthFactor;

    public static void main(String[] args) throws IOException {
        //TODO: multi threading, ForkJoinPool? Thread number Threshold?
        Validate.isTrue(args.length == NUM_OF_ARGS, IO_ERROR_MSG);
        String url = args[0];
        maxUrls = Integer.parseInt(args[1]);
        depthFactor = Integer.parseInt(args[2]);
        unique = Integer.parseInt(args[3]);
//        //TODO: verify validity of parameters
//        print("Fetching %s...", url);
//
//        Document doc = Jsoup.connect(url).get();
//        //TODO: save to file
        createDirectories(depthFactor);

        URLTask mainTask = new URLTask(url, 0, new AtomicInteger(0));
        mainTask.start();
//        urlToHtml(doc, url);
//
//        Elements links = doc.select("a[href]");
//        print("\nLinks: (%d)", links.size());
//        for (Element link : links) {
//            print(" * a: <%s>  (%s)", link.attr("abs:href"), trim(link.text(), 35));
//        }
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

            System.out.println("Error creating file ");
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

    private static class URLTask implements Runnable {
        private Thread _myThread;
        private final String _url;
        private final int _level;
        private AtomicInteger _filesCreatedByParent;
        private AtomicInteger _filesCreated;


        public URLTask(String url, int level, AtomicInteger filesCreatedByParent) {
            _url = url;
            _level = level;
            _filesCreatedByParent = filesCreatedByParent;
            _filesCreated = new AtomicInteger(0);
        }

        @Override
        public void run() {
            //add self
//            System.out.println("running url "+_myThread.getName());
            try {
                String fileName = _url
                        .replaceAll("https://", "")
                        .replaceAll(invalidCharRegex,
                                "_")
                        + ".html";
                Document doc = Jsoup.connect(_url).get();
                synchronized (fileNames) {
                    if (unique == 0 || !fileNames.contains(fileName)) {
                        fileNames.add(fileName);
                        fileName = String.valueOf(_level) + "/" + fileName;
                        urlToHtml(doc, fileName);
                        _filesCreatedByParent.getAndIncrement();
                    }
                }
                if (_level <= depthFactor) {
                    Elements links = doc.select("a[href]");
                    int pagesToLoad = Math.min(links.size(), maxUrls);
                    int i = 0;
                    for (Element link : links) {
                        if (_filesCreated.get() >= pagesToLoad || i >= links.size()) {
                            break;
                        }
                        String newUrl = link.attr("abs:href");
                        URLTask newTask = new URLTask(newUrl, _level + 1, _filesCreated);
                        newTask.start();
                        i++;
                    }
                }
            } catch (IOException e) {
                //TODO: something
            }

        }

        public void start() {
            if (_myThread == null) {
                _myThread = new Thread(this, _url);
                _myThread.start();
            }
        }

    }

}