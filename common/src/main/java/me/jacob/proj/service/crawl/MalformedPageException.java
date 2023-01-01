package me.jacob.proj.service.crawl;

public class MalformedPageException extends Exception {

    public MalformedPageException() {
        super();
    }

    public MalformedPageException(String message) {
        super(message);
    }

    public MalformedPageException(Throwable e) {
        super(e);
    }
}
