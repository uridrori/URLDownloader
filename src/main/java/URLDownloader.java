
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
import java.util.ArrayList;
import java.util.Arrays;


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

    public static void main(String[] args) throws IOException {
        Validate.isTrue(args.length == 4, IO_ERROR_MSG);
        String url = args[0];
        int maxUrls = Integer.parseInt(args[1]);
        int depthFactor = Integer.parseInt(args[2]);
        int crossLevel = Integer.parseInt(args[3]);
        //TODO: verify validity of parameters
        print("Fetching %s...", url);

        Document doc = Jsoup.connect(url).get();
        //TODO: save to file

        createDirectories(depthFactor);
        urlToHtml(doc, url);
        Elements links = doc.select("a[href]");
        Elements media = doc.select("[src]");
        Elements imports = doc.select("link[href]");

        print("\nMedia: (%d)", media.size());
        for (Element src : media) {
            if (src.normalName().equals("img"))
                print(" * %s: <%s> %sx%s (%s)",
                        src.tagName(), src.attr("abs:src"), src.attr("width"), src.attr("height"),
                        trim(src.attr("alt"), 20));
            else
                print(" * %s: <%s>", src.tagName(), src.attr("abs:src"));
        }

        print("\nImports: (%d)", imports.size());
        for (Element link : imports) {
            print(" * %s <%s> (%s)", link.tagName(), link.attr("abs:href"), link.attr("rel"));
        }

        print("\nLinks: (%d)", links.size());
        for (Element link : links) {
            print(" * a: <%s>  (%s)", link.attr("abs:href"), trim(link.text(), 35));
        }
    }

    private static void createDirectories(int depthFactor) {
        for (int i = 0; i<=depthFactor;i++)
        {
            try {

                Path path = Paths.get(String.valueOf(i));
                Files.createDirectory(path);
            }

            catch (IOException e){
                System.out.println("Error creating directory");
                e.printStackTrace();
            }
        }
    }


    private static void urlToHtml(Document doc, String url) {
        try {
            //TODO: remove https://
            String fileName = url.replaceAll("https://", "").replaceAll(invalidCharRegex, "_")+".html";
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
}