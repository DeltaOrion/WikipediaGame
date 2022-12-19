package me.jacob.proj.util;

import me.jacob.proj.crawl.FetchResult;
import me.jacob.proj.crawl.MalformedPageException;
import me.jacob.proj.crawl.analysis.TestAnalyzer;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestPage {

    public static void main(String[] args) throws IOException {
        TestPage testPage = TestPage.fromFile(new File("testpages").toPath().resolve("b").resolve("1.txt").toFile());
        System.out.println(testPage.title);
        System.out.println(testPage.link);
        System.out.println(testPage.description);
        System.out.println(testPage.links);
    }

    private final String link;
    private String title;
    private String description;
    private Set<String> links;

    public TestPage(String link) {
        this.link = link;
        this.title = "";
        this.description = "";
        this.links = new HashSet<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addLink(String link) {
        this.links.add(link);
    }

    public void addLink(TestPage page) {
        this.links.add(page.link);
    }

    public void removeLink(String link) {
        this.links.remove(link);
    }

    public Collection<String> getLinks() {
        return Collections.unmodifiableSet(links);
    }

    public WikiPage toPage() {
        WikiPage page = new WikiPage(title,new WikiLink("/wiki/"+link));
        page.setDescription(description);
        return page;
    }
    public String asString() {
        StringBuilder documentBuilder = new StringBuilder();
        documentBuilder.append("<p id = \"title\">"+title+"</p>");
        documentBuilder.append(System.lineSeparator());
        if(!description.isEmpty()) {
            documentBuilder.append("<p id = \"description\">"+description+"</p>");
            documentBuilder.append(System.lineSeparator());
        }

        for(String link : links) {
            documentBuilder.append("<a>"+link+"</a>");
            documentBuilder.append(System.lineSeparator());
        }

        return documentBuilder.toString();
    }
    public Document toDocument() {
        return Jsoup.parse(asString());
    }

    public File toFile(Path directory) throws IOException {
        File file = directory.resolve(link+".txt").toFile();
        file.createNewFile();

        try(FileWriter fileWriter = new FileWriter(file)) {
            try(BufferedWriter writer = new BufferedWriter(fileWriter)) {
                writer.write(asString());
            }
        }

        return file;
    }

    public static TestPage fromFile(File file) throws IOException {
        if(!file.exists())
            throw new IOException("File does not exist");

        Document document = Jsoup.parse(file);
        String fileFullName = file.getName();
        String name = fileFullName.substring(0,fileFullName.length()-4);

        WikiLink wikiLink = new WikiLink(name);
        FetchResult result = new FetchResult(wikiLink,document);

        TestAnalyzer analyzer = new TestAnalyzer();
        analyzer.setDocument(result);
        try {
            analyzer.analyze();
            WikiPage page = analyzer.getPage();
            TestPage testPage = new TestPage(name);
            testPage.setTitle(page.getTitle());
            testPage.setDescription(page.getDescription());
            for(WikiLink link : analyzer.getLinks()) {
                testPage.addLink(link.getRelative().split("/wiki/")[1]);
            }

            return testPage;
        } catch (MalformedPageException e) {
            throw new RuntimeException(e);
        }
    }
}
