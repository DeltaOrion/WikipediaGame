package me.jacob.proj.crawl.analysis.factory;

import me.jacob.proj.crawl.analysis.DocumentAnalyzer;
import me.jacob.proj.crawl.analysis.WikiDocumentAnalyzer;

public class WikiAnalyzerFactory implements AnalyzerFactory {

    @Override
    public DocumentAnalyzer get() {
        return new WikiDocumentAnalyzer();
    }
}
