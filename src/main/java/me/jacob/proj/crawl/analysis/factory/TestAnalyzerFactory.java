package me.jacob.proj.crawl.analysis.factory;

import me.jacob.proj.crawl.analysis.DocumentAnalyzer;
import me.jacob.proj.crawl.analysis.TestAnalyzer;

public class TestAnalyzerFactory implements AnalyzerFactory {
    @Override
    public DocumentAnalyzer get() {
        return new TestAnalyzer();
    }
}
