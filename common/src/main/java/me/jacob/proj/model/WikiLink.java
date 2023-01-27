package me.jacob.proj.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class WikiLink {

    private final static String PROTOCOL = "https";
    private final static String AUTHORITY = "wikipedia.org";
    private final Map<Locale,String> paths;

    public static void main(String[] args) throws MalformedURLException {
        WikiLink l = new WikiLink(new URL("https://en.wikipedia.org/wiki/Black_hole"));
        l.addSupportedLang(Locale.ITALIAN,"/wiki/Black_hole");
        System.out.println(l.getURLS());
    }

    public WikiLink(URL page) {
        this(page.getPath());
    }

    public WikiLink(Map<Locale,String> authorities) {
        if(!authorities.containsKey(Locale.ENGLISH))
            throw new IllegalArgumentException("no english link");

        this.paths = authorities;
    }

    public WikiLink(String page) {
        this.paths = new HashMap<>();
        this.paths.put(Locale.ENGLISH,page);
    }

    public void addSupportedLang(Locale locale, String path) {
        paths.put(locale,path);
    }

    public Collection<Locale> getSupportedLangs() {
        return Collections.unmodifiableSet(paths.keySet());
    }

    public URL getLink() {
        return getLink(Locale.ENGLISH);
    }

    public URL getLink(Locale locale) {
        try {
            return new URL(PROTOCOL, locale.getLanguage() + "." + AUTHORITY, paths.get(locale));
        } catch (MalformedURLException e) {
            throw new RuntimeException();
        }
    }

    public Collection<URL> getURLS() {
        List<URL> urls = new ArrayList<>();
        for(Locale locale : paths.keySet()) {
            urls.add(getLink(locale));
        }

        return urls;
    }

    public String getRelative() {
        return getRelative(Locale.ENGLISH);
    }

    public String getRelative(Locale locale) {
        return paths.get(locale);
    }

    @Override
    public String toString() {
        return getLink().toString();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof WikiLink))
            return false;

        WikiLink link = (WikiLink) o;
        return this.paths.get(Locale.ENGLISH).equals(link.paths.get(Locale.ENGLISH));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(paths.get(Locale.ENGLISH));
    }

    public Map<Locale, String> getAuthorities() {
        return Collections.unmodifiableMap(paths);
    }
}
