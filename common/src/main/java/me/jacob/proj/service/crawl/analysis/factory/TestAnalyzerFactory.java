package me.jacob.proj.service.crawl.analysis.factory;

import me.jacob.proj.service.crawl.analysis.DocumentAnalyzer;
import me.jacob.proj.service.crawl.analysis.TestAnalyzer;

public class TestAnalyzerFactory implements AnalyzerFactory {
    @Override
    public DocumentAnalyzer get() {
        return new TestAnalyzer();
    }
}
