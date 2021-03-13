
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;


//TODO: documentation
//TODO: split to another module?
//TODO: remove prints
//TODO: public before private

/**
 * A one-class program which downloads all HTML links from a given URL (including itself). Search is limited in
 * depth, amount of pages and cross-level duplicates by the program arguments.
 */
public class URLDownloader {

    //constants
    private static final int NUM_OF_ARGS = 4;
    private static final String URL_REGEX = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    private static final String POSITIVE_INT_REGEX = "[0-9]+";
    private static final String IO_ERR_MSG = "Please supply:" + "\n"
            + "1. URL to scrap" + "\n"
            + "2. Maximal number of URLs to fetch" + "\n"
            + "3. Search depth factor" + "\n"
            + "4. 1 for cross-level uniqueness, 0 otherwise";
    private static final String INVALID_CHAR_REGEX = "[,\\.<>:\"/|?*]";
    private static final HashSet<String> fileNames = new HashSet<>();
    private static final String CREATE_DIRECTORY_ERR_MSG = "Error creating directory ";
    private static final String CREATE_FILE_ERR_MSG = "Error creating file ";

    private static int maxUrls;
    private static int unique;
    private static int depthFactor;

    /**
     * The main function of the program. Checks validity of input parameters and upon success creates and starts
     * the main thread. If successful - downloads all files to level sub-directories according to the limitations on
     * depth and number of URLs, otherwise prints an informative error message.
     * @param args An array of the program arguments.
     */
    public static void main(String[] args) {
        try {
            Validate.isTrue(args.length == NUM_OF_ARGS, IO_ERR_MSG);
            if (!checkInputValidity(args[0], args[1], args[2], args[3])) {
                throw new IOException();
            }
            String rootUrl = args[0];
            maxUrls = Integer.parseInt(args[1]);
            depthFactor = Integer.parseInt(args[2]);
            unique = Integer.parseInt(args[3]);
            createDirectories(depthFactor);
            URLTask mainTask = new URLTask(rootUrl, 0, new AtomicInteger(0));
            mainTask.start();
        } catch (IOException e) {
            System.out.println(IO_ERR_MSG);
            e.printStackTrace();
        }
    }

    /**
     * Checks whether the input is in the correct format.
     * @param rootUrl A string representing a URL address.
     * @param maxUrls A string of a positive integer.
     * @param depthFactor A string of a positive integer.
     * @param unique A binary digit
     * @return true if all parameters fit the above description, false otherwise.
     */
    public static boolean checkInputValidity(String rootUrl, String maxUrls, String depthFactor, String unique) {
        return (rootUrl.matches(URL_REGEX)
                && maxUrls.matches(POSITIVE_INT_REGEX)
                && depthFactor.matches(POSITIVE_INT_REGEX) && unique.matches("[0,1]"));
    }

    /**
     * Creates a directory for each level of depth.
     * @param depthFactor the depth level of the search
     */
    private static void createDirectories(int depthFactor) {
        for (int i = 0; i <= depthFactor; i++) {
            try {
                Path path = Paths.get(String.valueOf(i));
                if (Files.isDirectory(path)) {
                    continue;
                }
                Files.createDirectory(path);
            } catch (IOException e) {
                System.out.println(CREATE_DIRECTORY_ERR_MSG + i);
                e.printStackTrace();
            }
        }
    }


    /**
     * Writes the contents of a url to an html document
     * @param doc - A Document object
     * @param fileName is the name of the file to be created
     */
    private static void urlToHtml(Document doc, String fileName) {
        try {
            File newFile = new File(fileName);
            if (newFile.exists()) {
                System.out.println(fileName + " already exists");
            } else if (newFile.createNewFile()) {
                writeToFile(doc, fileName);
            } else {
                System.out.println(fileName + " already exists");
            }
        } catch (IOException e) {

            System.out.println(CREATE_FILE_ERR_MSG + fileName);
            e.printStackTrace();
        }
    }

    private static void writeToFile(Document doc, String fileName) {
        try {
            FileWriter myWriter = new FileWriter(fileName);
            myWriter.write(doc.html());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class URLTask implements Runnable {
        //constants
        private static final String URL_ERROR_MSG = "Error accessing one of the pages in ";
        private static final String LINK_EXPRESSION = "a[href]";
        private static final String URL_ATTRIBUTE_KEY = "abs:href";
        //For Linux/Unix - change to backslash
        private static final String SLASH = "/";

        private Thread _myThread;
        private final String _url;
        private final int _level;
        private final AtomicInteger _filesCreatedByParent;
        private final AtomicInteger _filesCreated;


        public URLTask(String url, int level, AtomicInteger filesCreatedByParent) {
            _url = url;
            _level = level;
            _filesCreatedByParent = filesCreatedByParent;
            _filesCreated = new AtomicInteger(0);
        }

        @Override
        public void run() {
            try {
                String fileName = fileNameFromUrl();
                Document doc = Jsoup.connect(_url).get();
                synchronized (fileNames) {
                    if (unique == 0 || !fileNames.contains(fileName)) {
                        addFile(fileName, doc);
                    }
                }
                if (_level < depthFactor) {
                    downloadChildren(doc);
                }
            } catch (IOException e) {
                System.out.println(URL_ERROR_MSG + _url);
            }

        }

        public void start() {
            if (_myThread == null) {
                _myThread = new Thread(this, _url);
                _myThread.start();
            }
        }

        private void downloadChildren(Document doc) {
            Elements links = doc.select(LINK_EXPRESSION);
            for (Element link : links) {
                if (_filesCreated.get() > maxUrls) {
                    break;
                }
                String newUrl = link.attr(URL_ATTRIBUTE_KEY);
                if (newUrl.equals("")) {
                    continue;
                }
                URLTask newTask = new URLTask(newUrl, _level + 1, _filesCreated);
                _filesCreated.getAndIncrement();
                newTask.start();
            }
        }

        private String fileNameFromUrl() {
            return _url
                    .replaceAll("https://", "")
                    .replaceAll("http://", "")
                    .replaceAll(INVALID_CHAR_REGEX, "_")
                    + ".html";
        }

        private void addFile(String fileName, Document doc) {
            fileNames.add(fileName);
            fileName = _level + SLASH + fileName;
            urlToHtml(doc, fileName);
            _filesCreatedByParent.getAndIncrement();
        }

    }

}