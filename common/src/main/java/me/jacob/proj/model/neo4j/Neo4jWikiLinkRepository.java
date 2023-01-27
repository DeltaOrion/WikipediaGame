package me.jacob.proj.model.neo4j;

import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;

import java.util.*;
import java.util.regex.Pattern;

public class Neo4jWikiLinkRepository {

    private final static Pattern COLON = Pattern.compile(":");

    public Result writeLink(SimpleQueryRunner transaction, WikiLink link) {
        List<WikiLink> links = new ArrayList<>();
        links.add(link);
        return writeMany(transaction,links);
    }

    public Result writeMany(SimpleQueryRunner tx, Collection<WikiLink> links) {
        List<Map<String,Object>> params = new ArrayList<>();
        for(WikiLink link : links) {
            Map<String, Object> linkParams = new HashMap<>();
            linkParams.put("en", link.getRelative());
            List<String> mapArr = new ArrayList<>();
            for(Map.Entry<Locale,String> authority : link.getAuthorities().entrySet()) {
                String key = authority.getKey().toString();
                String value = authority.getValue();
                mapArr.add(key+":"+value);
            }
            linkParams.put("locales",mapArr);
            params.add(linkParams);
        }

        Map<String,Object> linkMap = new HashMap<>();
        linkMap.put("links",params);

        Query query = new Query(
                "UNWIND $links AS data " +
                "MERGE (link:WIKILINK {en:data.en}) " +
                "ON CREATE SET link.en=data.en,link.locales=data.locales " +
                "ON MATCH SET link.en=data.en,link.locales=data.locales " +
                "RETURN link",linkMap);

        return tx.run(query);
    }

    public WikiLink resolveLink(Node wikilinkNode) {
        Map<Locale,String> authorities = new HashMap<>();
        authorities.put(Locale.ENGLISH,wikilinkNode.get("en").asString());
        for(Value authority : wikilinkNode.get("locales").values()) {
            String[] pair = COLON.split(authority.toString());
            if(pair.length<2)
                continue;

            Locale locale = new Locale(pair[0]);
            authorities.put(locale,pair[1]);
        }

        return new WikiLink(authorities);
    }
}
