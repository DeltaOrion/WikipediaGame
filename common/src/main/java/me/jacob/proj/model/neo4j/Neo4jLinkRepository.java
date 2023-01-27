package me.jacob.proj.model.neo4j;

import me.jacob.proj.model.CrawlableLink;
import me.jacob.proj.model.LinkRepository;
import me.jacob.proj.model.WikiLink;
import me.jacob.proj.model.WikiPage;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;

import java.util.*;

public class Neo4jLinkRepository implements LinkRepository {

    private final Driver driver;
    private final Neo4jWikiLinkRepository linkRepo;
    private final Neo4jPageRepository pageRepo;

    public Neo4jLinkRepository(Driver driver, Neo4jPageRepository pageRepo) {
        this.driver = driver;
        this.pageRepo = pageRepo;
        this.linkRepo = new Neo4jWikiLinkRepository();
    }

    public static void main(String[] args) {
        Driver driver = GraphDatabase.driver("bolt://localhost:7687",AuthTokens.basic("neo4j","password"));
        Neo4jPageRepository pageRepo = new Neo4jPageRepository(driver);
        Neo4jLinkRepository linkRepo = new Neo4jLinkRepository(driver,pageRepo);

        WikiLink link = new WikiLink("gaming");
        WikiLink link2 = new WikiLink("french");
        CrawlableLink cLink = new CrawlableLink(link,UUID.randomUUID(), pageRepo);
        cLink.toggleProcessed(true);
        cLink.setRegistered(false);
        cLink.setPageFound(true);

        WikiPage gaming = new WikiPage("gaming",link,pageRepo);
        gaming.setUniqueId(0);
        WikiPage french = new WikiPage("french",link2,pageRepo);
        gaming.setUniqueId(1);

        cLink.addUnconnected(gaming);
        cLink.addUnconnected(french);

        linkRepo.create(cLink);

        CrawlableLink frenchLink = linkRepo.getOrMake(link2);
        System.out.println(frenchLink);

        System.out.println(linkRepo.getAll());
        System.out.println(linkRepo.getBefore(1000));
        System.out.println(linkRepo.get(link));

        System.out.println(linkRepo.get(cLink.getLink()).getUnconnected());
    }

    @Override
    public CrawlableLink getOrMake(WikiLink link) {
        List<WikiLink> links = new ArrayList<>();
        links.add(link);
        try (Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                List<CrawlableLink> crawlableLinks = new ArrayList<>(getOrMakeMany(tx, links));
                tx.commit();
                return crawlableLinks.get(0);
            }
        }
    }

    private CrawlableLink getFirst(Session session, Query query) {
        List<CrawlableLink> links = getLinks(session,query);
        if(links.size()==0)
            return null;

        return links.get(0);
    }

    private List<CrawlableLink> getLinks(SimpleQueryRunner session, Query query) {
        Result rs = session.run(query);
        List<CrawlableLink> results = new ArrayList<>();
        while (rs.hasNext()) {
            Record record = rs.next();
            Node wikilinkNode = record.get("link").asNode();
            Node wikipageNode = record.get("clink").asNode();

            // create the wikilink object
            WikiLink link = linkRepo.resolveLink(wikilinkNode);

            // create the wikipage object
            CrawlableLink clink = resolveLink(wikipageNode,link);
            // do something with the wikilink and wikipage objects
            //get
            results.add(clink);
        }


        return results;
    }

    private CrawlableLink resolveLink(Node clinkNode, WikiLink link) {
        UUID uniqueId = UUID.fromString(clinkNode.get("id").asString());
        long lastProcessed = clinkNode.get("lastProcessed").asLong();
        boolean pageFound = clinkNode.get("pageFound").asBoolean();
        boolean processed = clinkNode.get("processed").asBoolean();
        boolean registered = clinkNode.get("registered").asBoolean();

        CrawlableLink clink = new CrawlableLink(link,uniqueId, pageRepo);
        clink.setLastProcessed(lastProcessed);
        clink.setPageFound(pageFound);
        clink.setProcessed(processed);
        clink.setRegistered(registered);

        return clink;
    }

    @Override
    public Collection<CrawlableLink> getOrMake(Collection<WikiLink> links) {
        try (Session session = driver.session()) {
            try(Transaction tx = session.beginTransaction()) {
                Collection<CrawlableLink> cLinks = getOrMakeMany(tx,links);
                tx.commit();
                return cLinks;
            }
        }
    }

    private Collection<CrawlableLink> getOrMakeMany(Transaction tx, Collection<WikiLink> links) {
        List<Map<String, Object>> linkParamters = new ArrayList<>();
        Map<String,Object> parameters = new HashMap<>();
        parameters.put("links",linkParamters);

        linkRepo.writeMany(tx,links);

        for(WikiLink link : links) {
            CrawlableLink newLink = new CrawlableLink(link, UUID.randomUUID(), pageRepo);
            Map<String,Object> linkParam = fillParameters(newLink);
            linkParam.put("en", link.getRelative());
            linkParamters.add(linkParam);
        }

        String setProperties = "{id:data.id, processed:data.processed, registered:data.registered, pageFound:data.pageFound, lastProcessed:data.lastProcessed}";

        Query query = new Query("UNWIND $links AS data " +
                "MATCH (link:WIKILINK {en: data.en})" +
                "MERGE (link) -[:LINKOF]-> (clink:CRAWLABLELINK) " +
                "ON CREATE SET clink += " + setProperties + " " +
                "RETURN clink,link",parameters);

        return getLinks(tx,query);
    }

    @Override
    public void updateAll(Collection<CrawlableLink> links, boolean updateConnected) {
        try(Session session = driver.session()) {
            batchWrite(session,links,true,true);
        }
    }

    //performs batch if exists update else create
    private void batchWrite(Session session, Collection<CrawlableLink> links, boolean updateConnected, boolean updateLink) {
        try (Transaction tx = session.beginTransaction()) {
            if(updateLink) {
                List<WikiLink> wikiLinks = new ArrayList<>();
                for(CrawlableLink link : links) {
                    wikiLinks.add(link.getLink());
                }

                linkRepo.writeMany(tx, wikiLinks);
            }

            String setProperties = "{id:data.id, processed:data.processed, registered:data.registered, pageFound:data.pageFound, lastProcessed:data.lastProcessed}";
            List<Map<String, Object>> params = new ArrayList<>();
            for(CrawlableLink link : links) {
                Map<String,Object> linkParameters = fillParameters(link);
                linkParameters.put("en", link.getLink().getRelative());
                params.add(linkParameters);
            }

            Map<String,Object> queryParams = new HashMap<>();
            queryParams.put("links",params);

            Query linkQuery = new Query("UNWIND $links AS data " +
                    "MATCH (link:WIKILINK {en: data.en}) " +
                    "MERGE (link) -[:LINKOF]-> (clink:CRAWLABLELINK) " +
                    "ON MATCH SET clink += " + setProperties +
                    "ON CREATE SET clink += " + setProperties + " " +
                    "RETURN clink", queryParams);

            tx.run(linkQuery);

            if(updateLink) {
                saveRelationships(tx, links);
            }

            tx.commit();
        }


        /**
         * TODO
         *  - I think this is dodgy and is causing the deadlock
         *  - The fact that the transaction ends earlier and
         *  - relationships are still being updated
         */
        if(updateConnected) {
            Set<WikiPage> create = new HashSet<>();
            for(CrawlableLink link : links) {
                create.addAll(link.getCachedUnconnected());
            }
            pageRepo.createPages(create);
            pageRepo.saveUnconnected(links);
        }
    }


    @Override
    public CrawlableLink get(WikiLink link) {
        try(Session session = driver.session()) {
            Map<String,Object> params = new HashMap<>();
            params.put("en",link.getRelative());
            Query query = new Query("MATCH (link:WIKILINK {en:$en}) -[:LINKOF]-> (clink:CRAWLABLELINK) " +
                    "RETURN link,clink",params);

            return getFirst(session,query);
        }
    }

    @Override
    public Collection<CrawlableLink> getAll() {
        try(Session session = driver.session()) {
            Query query = new Query("MATCH (link:WIKILINK) -[:LINKOF]-> (clink:CRAWLABLELINK) " +
                    "RETURN link,clink");

            return getLinks(session,query);
        }
    }

    @Override
    public void update(CrawlableLink link, boolean updateConnected) {
        try (Session session = driver.session()) {
            List<CrawlableLink> links = new ArrayList<>();
            links.add(link);
            batchWrite(session,links,updateConnected,false);
        }
    }

    @Override
    public void create(CrawlableLink link) {
        try (Session session = driver.session()) {
            List<CrawlableLink> links = new ArrayList<>();
            links.add(link);
            batchWrite(session,links,true,true);
        }
    }

    private void saveRelationships(SimpleQueryRunner tx, Collection<CrawlableLink> links) {
        List<Map<String,Object>> params = new ArrayList<>();
        for(CrawlableLink link : links) {
            Map<String,Object> relationshipParams = new HashMap<>();
            relationshipParams.put("en", link.getLink().getRelative());
            relationshipParams.put("id", link.getUniqueId().toString());
            params.add(relationshipParams);
        }

        Map<String,Object> queryParams = new HashMap<>();
        queryParams.put("links",params);
        Query relationshipQuery = new Query("UNWIND $links AS data " +
                "MATCH (clink:CRAWLABLELINK {id:data.id}),(link:WIKILINK {en:data.en}) " +
                "MERGE (link)-[:LINKOF]->(clink) " +
                "RETURN link,clink",queryParams);

        tx.run(relationshipQuery);
    }

    private Map<String, Object> fillParameters(CrawlableLink link) {
        Map<String,Object> map = new HashMap<>();
        map.put("id",link.getUniqueId().toString());
        map.put("processed",link.isProcessed());
        map.put("registered",link.isRegistered());
        map.put("pageFound",link.isPageFound());
        map.put("lastProcessed",link.getLastProcessed());
        return map;
    }

    @Override
    public void delete(WikiLink link) {
        try (Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                Map<String,Object> params = new HashMap<>();
                params.put("en",link.getRelative());
                Query query = new Query("MATCH (link:WIKILINK {en:$en}) -[:LINKOF]-> (clink:CRAWLABLELINK) " +
                        "DETACH DELETE clink",params);

                tx.run(query);
                tx.commit();
            }
        }
    }


    @Override
    public int getAmountOfLinks() {
        try(Session session = driver.session()) {
            Query query = new Query("MATCH (n:CRAWLABLELINK) RETURN COUNT(n)");
            Result rs = session.run(query);
            Record record = rs.single();
            return record.get(0).asInt();
        }
    }

    @Override
    public Collection<CrawlableLink> getBefore(long time) {
        try(Session session = driver.session()) {
            Map<String, Object> params = new HashMap<>();
            params.put("time", time);
            Query query = new Query("MATCH (link:WIKILINK) -[:LINKOF]-> (clink:CRAWLABLELINK) " +
                    "WHERE clink.lastProcessed < $time " +
                    "RETURN link,clink",params);

            return getLinks(session,query);
        }
    }
}
