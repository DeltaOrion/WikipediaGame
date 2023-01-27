package me.jacob.proj.model.neo4j;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.model.PageRepository;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Neo4jPageRepository implements PageRepository {

    private final Driver driver;
    private final Neo4jWikiLinkRepository linkRepo = new Neo4jWikiLinkRepository();
    private final AtomicInteger count;

    public Neo4jPageRepository(Driver driver) {
        this.driver = driver;
        this.count = new AtomicInteger(getAmountOfPages());
    }

    public static void main(String[] args) {
        Driver driver = GraphDatabase.driver("bolt://localhost:7687",AuthTokens.basic("neo4j","password"));
        Neo4jPageRepository pageRepo = new Neo4jPageRepository(driver);
        for (WikiPage page : pageRepo.getAllPages()) {
            System.out.println(page.getTitle() + " " + page.getUniqueId());
            for (WikiPage neighbour : page.getNeighbours()) {
                System.out.println(" - " + neighbour.getTitle());
            }
        }


                /*
        WikiLink link = new WikiLink("United_Kingdom");
        link.addSupportedLang(Locale.FRANCE,"Kingdom De Unite");
        link.addSupportedLang(Locale.GERMAN,"Beat_Us_In_WW2_:(");

        WikiPage unitedKingdom = new WikiPage("United Kingdom",link,pageRepo);
        unitedKingdom.setUniqueId(1);
        unitedKingdom.setDescription("abc");
        unitedKingdom.setRedirect(false);
        unitedKingdom.setArticleType("Article");
        unitedKingdom.setRemoved(false);
        pageRepo.createPage(unitedKingdom);

        WikiLink link2 = new WikiLink("The_Queen");
        WikiPage theQueen = new WikiPage("The Queen",link2, pageRepo);
        theQueen.setUniqueId(2);
        theQueen.setDescription("Gaming");
        theQueen.setRedirect(false);
        theQueen.setArticleType("Article");
        theQueen.setRemoved(false);

        unitedKingdom.addNeighbour(theQueen);

        System.out.println(pageRepo.getPage("United Kingdom"));
        unitedKingdom.setDescription("Gaming");
        unitedKingdom.setRemoved(true);

        pageRepo.savePage(unitedKingdom);

        List<WikiPage> batch = new ArrayList<>();
        batch.add(theQueen);
        batch.add(unitedKingdom);

        pageRepo.createPages(batch);

         */
    }

    @Override
    public WikiPage getPage(String title) {
        try(Session session = driver.session()) {
            Map<String,Object> parameters = new HashMap<>();
            parameters.put("title",title);
            Query query = new Query("MATCH (link:WIKILINK) -[:URL]-> (page:WIKIPAGE {title:$title}) RETURN link,page",parameters);
            return getFirst(session,query);
        }
    }

    @Override
    public WikiPage getPage(int id) {
        try(Session session = driver.session()) {
            Map<String,Object> parameters = new HashMap<>();
            parameters.put("id",id);
            Query query = new Query("MATCH (link:WIKILINK) -[:URL]-> (page:WIKIPAGE {id:$id}) RETURN link,page",parameters);
            return getFirst(session,query);
        }
    }

    @Override
    public WikiPage getPage(WikiLink link) {
        try(Session session = driver.session()) {
            Map<String,Object> parameters = new HashMap<>();
            parameters.put("en",link.getRelative());
            Query query = new Query("MATCH (link:WIKILINK {en:$en}) -[:URL]-> (page:WIKIPAGE) RETURN link,page",parameters);
            return getFirst(session,query);
        }
    }


    @Override
    public Collection<WikiPage> getAllPages() {
        try(Session session = driver.session()) {
            Query query = new Query("MATCH (link:WIKILINK) -[:URL]-> (page:WIKIPAGE) RETURN link,page");
            return getPages(session,query);
        }
    }

    @Override
    public Collection<WikiPage> getAll(Collection<WikiLink> links) {
        try(Session session = driver.session()) {
            List<String> l = new ArrayList<>();
            for(WikiLink link : links) {
                l.add(link.getRelative());
            }
            Map<String,Object> params = new HashMap<>();
            params.put("wikilinks",l);
            Query query = new Query("UNWIND $wikilinks AS wl MATCH (link:WIKILINK)-[:URL]->(page:WIKIPAGE) WHERE link.en=wl RETURN link,page",params);
            return getPages(session,query);
        }
    }

    @Override
    public Collection<WikiPage> getNeighbours(int id) {
        try(Session session = driver.session()) {
            Map<String,Object> parameters = new HashMap<>();
            parameters.put("id",id);
            Query query = new Query("MATCH (base:WIKIPAGE {id:$id}) -[:HYPERLINKS]-> (page:WIKIPAGE),(link:WIKILINK) -[:URL]->(page)  RETURN page,link",parameters);
            return getPages(session,query);
        }
    }

    private WikiPage getFirst(Session session, Query query) {
        List<WikiPage> pages = getPages(session,query);
        if(pages.size()==0)
            return null;

        return pages.get(0);
    }

    private List<WikiPage> getPages(Session session, Query query) {
        Result rs = session.run(query);
        List<WikiPage> results = new ArrayList<>();
        while (rs.hasNext()) {
            Record record = rs.next();
            Node wikilinkNode = record.get("link").asNode();
            Node wikipageNode = record.get("page").asNode();

            // create the wikilink object
            WikiLink link = linkRepo.resolveLink(wikilinkNode);

            // create the wikipage object
            WikiPage page = resolvePage(wikipageNode,link);

            // do something with the wikilink and wikipage objects
            results.add(page);
        }
        return results;
    }

    private WikiPage resolvePage(Node wikipageNode, WikiLink link) {
        WikiPage wikipage = new WikiPage(wikipageNode.get("title").asString(),link,this);
        wikipage.setUniqueId(wikipageNode.get("id").asInt());
        wikipage.setDescription(wikipageNode.get("description").asString());
        wikipage.setRedirect(wikipageNode.get("redirect").asBoolean());
        wikipage.setArticleType(wikipageNode.get("articleType").asString());
        wikipage.setRemoved(wikipageNode.get("isRemoved").asBoolean());
        return wikipage;
    }

    @Override
    public void createPage(WikiPage page) {
        try(Session session = driver.session()) {
            try(Transaction tx = session.beginTransaction()) {
                // Run relationshipQuery to save the Wiki Link
                writeSingle(tx,page,true,true);
                tx.commit();
            }
        }
    }

    @Override
    public void createPages(Collection<WikiPage> pages) {
        try (Session session = driver.session()) {
            //fake batch update
            try(Transaction tx = session.beginTransaction()) {
                for(WikiPage page : pages) {
                    writeSingle(tx,page,true,true);
                }
                tx.commit();
            }
        }
    }

    private void writeMany(SimpleQueryRunner tx, Collection<WikiPage> pages, boolean writeNeighbours, boolean writeLinks) {
        if(writeLinks) {
            List<WikiLink> links = new ArrayList<>();
            for(WikiPage page : pages) {
                links.add(page.getLink());
            }
            linkRepo.writeMany(tx, links);
        }

        Map<String,Object> params = new HashMap<>();
        List<Map<String,Object>> allPages = new ArrayList<>();
        for(WikiPage page : pages) {
            allPages.add(fillParameters(page));
        }
        params.put("pages",allPages);
        String setProperties = "{id:data.id, title:data.title, description:data.description, redirect:data.redirect, articleType:data.articleType, isRemoved:data.isRemoved}";

        Query pageQuery = new Query("UNWIND $pages AS data " +
                "MERGE (page:WIKIPAGE {id:data.id}) " +
                "ON MATCH SET page += " + setProperties +
                "ON CREATE SET page += " + setProperties + " " +
                "RETURN page",params);

        tx.run(pageQuery);

        if(writeLinks)
            saveRelationships(tx,pages);

        if(writeNeighbours) {
            updateNeighbours(tx, pages);
        }
    }

    private void writeSingle(SimpleQueryRunner tx, WikiPage page, boolean writeNeigbours, boolean writeLink) {
        List<WikiPage> pages = new ArrayList<>();
        pages.add(page);
        writeMany(tx,pages,writeNeigbours,writeLink);
    }

    private void saveRelationships(SimpleQueryRunner tx , Collection<WikiPage> pages) {
        Map<String,Object> params = new HashMap<>();
        List<Map<String,Object>> pageMap = new ArrayList<>();
        for(WikiPage page : pages) {
            Map<String,Object> relationshipParams = new HashMap<>();
            relationshipParams.put("en", page.getLink().getRelative());
            relationshipParams.put("id", page.getUniqueId());
            pageMap.add(relationshipParams);
        }
        params.put("pages",pageMap);

        Query relationshipQuery = new Query("UNWIND $pages AS data " +
                "MATCH (page:WIKIPAGE {id:data.id}),(link:WIKILINK {en:data.en})" +
                "MERGE (link)-[:URL]->(page)" +
                "RETURN page,link",params);

        tx.run(relationshipQuery);
    }

    private Map<String, Object> fillParameters(WikiPage page) {
        Map<String,Object> pageParameters = new HashMap<>();
        pageParameters.put("id",page.getUniqueId());
        pageParameters.put("title",page.getTitle());
        pageParameters.put("description",page.getDescription());
        pageParameters.put("redirect",page.isRedirect());
        pageParameters.put("articleType",page.getArticleType());
        pageParameters.put("isRemoved",page.isRemoved());
        return pageParameters;
    }

    @Override
    public void savePage(WikiPage page, boolean updateLinks) {
        //write method with one element
        try(Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                // Run relationshipQuery to save the Wiki Link
                writeSingle(tx, page, updateLinks,false);
                tx.commit();
            }
        }
    }

    private void updateNeighbours(SimpleQueryRunner tx, Collection<WikiPage> pages) {
        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> pageParams = new ArrayList<>();
        for (WikiPage page : pages) {
            Map<String, Object> pageParam = new HashMap<>();
            List<Map<String, Object>> neighbourParams = new ArrayList<>();
            for (WikiPage neighbour : page.getCachedNeighbours()) {
                neighbourParams.add(fillParameters(neighbour));
            }
            pageParam.put("id", page.getUniqueId());
            pageParam.put("neighbours", neighbourParams);
            pageParams.add(pageParam);
        }
        params.put("pages", pageParams);

        String neighbourProperties = "{id:neighbour.id, title:neighbour.title, description:neighbour.description, redirect:neighbour.redirect, articleType:neighbour.articleType, isRemoved:neighbour.isRemoved}";

        Query neighbourQuery = new Query("UNWIND $pages as page " +
                "UNWIND page.neighbours as neighbour " +
                "MATCH (pageNode:WIKIPAGE {id: page.id}) " +
                "MERGE (neighbourNode:WIKIPAGE {id: neighbour.id}) " +
                "ON MATCH SET neighbourNode += " + neighbourProperties +
                "ON CREATE SET neighbourNode +=  " + neighbourProperties +
                "MERGE (pageNode)-[:HYPERLINKS]->(neighbourNode)", params);

        tx.run(neighbourQuery);
    }

    @Override
    public void updateName(String oldTitle, WikiPage page) {
        savePage(page,false);
    }

    @Override
    public int getAmountOfPages() {
        try(Session session = driver.session()) {
            Query query = new Query("MATCH (n:WIKIPAGE) RETURN COUNT(n)");
            Result rs = session.run(query);
            Record record = rs.single();
            return record.get(0).asInt();
        }
    }

    @Override
    public int nextUniqueId() {
        return count.getAndIncrement();
    }

    @Override
    public void clearNeighbours(int uniqueId) {
        try(Session session = driver.session()) {
            Map<String,Object> params = new HashMap<>();
            params.put("id",uniqueId);
            Query query = new Query("MATCH (page:WIKIPAGE {id:$id}) -[r:HYPERLINKS]-> (neighbour:WIKIPAGE) DELETE r",params);
            Result rs = session.run(query);
        }
    }

    @Override
    public Collection<WikiPage> getAndClearUnconnected(UUID uniqueId) {
        try(Session session = driver.session()) {
            Map<String,Object> params = new HashMap<>();
            params.put("id",uniqueId.toString());
            Query query = new Query("MATCH (clink:CRAWLABLELINK {id: $id}) -[r:UNCONNECTED]-> (page:WIKIPAGE),(link:WIKILINK) -[:URL]-> (page) " +
                    "DELETE r " +
                    "RETURN link,page",params);
            return getPages(session,query);
        }
    }

    @Override
    public Collection<WikiPage> getUnconnected(UUID uniqueId) {
        try(Session session = driver.session()) {
            Map<String,Object> params = new HashMap<>();
            params.put("id",uniqueId.toString());
            Query query = new Query("MATCH (clink:CRAWLABLELINK {id: $id}) -[:UNCONNECTED]-> (page:WIKIPAGE),(link:WIKILINK) -[:URL]-> (page) " +
                    "RETURN link,page",params);

            return getPages(session,query);
        }
    }

    @Override
    public void saveUnconnected(Collection<CrawlableLink> links) {
        try (Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                Map<String, Object> edgesParams = new HashMap<>(); //top level
                List<Map<String, Object>> unconnectedParams = new ArrayList<>(); //links
                for(CrawlableLink link : links) {
                    List<Integer> unconnected = new ArrayList<>();
                    for (WikiPage node : link.getCachedUnconnected()) {
                        unconnected.add(node.getUniqueId());
                    }
                    Map<String,Object> params = new HashMap<>();
                    params.put("unconnected",unconnected);
                    params.put("id",link.getUniqueId().toString());
                    unconnectedParams.add(params);
                }

                edgesParams.put("links",unconnectedParams);

                Query neighbourQuery = new Query("UNWIND $links AS data " +
                        "MATCH (link:CRAWLABLELINK {id: data.id})"
                        + "UNWIND data.unconnected as unconnectedId "
                        + "MATCH (page:WIKIPAGE {id: unconnectedId})"
                        + "MERGE (link)-[:UNCONNECTED]->(page)", edgesParams);

                tx.run(neighbourQuery);
                tx.commit();
            }
        }
    }
}
