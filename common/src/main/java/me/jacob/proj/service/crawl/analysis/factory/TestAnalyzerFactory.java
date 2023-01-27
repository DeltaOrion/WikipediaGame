package me.jacob.proj.service.crawl.analysis.factory;

import me.jacob.proj.service.Wikipedia;
import me.jacob.proj.service.crawl.analysis.DocumentAnalyzer;
import me.jacob.proj.service.crawl.analysis.TestAnalyzer;

public class TestAnalyzerFactory implements AnalyzerFactory {

    private final Wikipedia wikipedia;

    public TestAnalyzerFactory(Wikipedia wikipedia) {
        this.wikipedia = wikipedia;
    }

    @Override
    public DocumentAnalyzer get() {
        return new TestAnalyzer(wikipedia);
    }
}
