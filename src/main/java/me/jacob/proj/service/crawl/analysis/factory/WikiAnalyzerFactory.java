package me.jacob.proj.service.crawl.analysis.factory;

import me.jacob.proj.service.crawl.analysis.DocumentAnalyzer;
import me.jacob.proj.service.crawl.analysis.WikiDocumentAnalyzer;

public class WikiAnalyzerFactory implements AnalyzerFactory {

    @Override
    public DocumentAnalyzer get() {
        return new WikiDocumentAnalyzer();
    }
}
