package me.jacob.proj.view;

import me.jacob.proj.model.WikiPage;
import me.jacob.proj.service.Wikipedia;

import java.util.List;

public class DisplayWikiView {

    public void displayPages(Wikipedia wikipedia) {
        for(WikiPage page : wikipedia.getAllPages()) {
            System.out.println(page.getTitle());
            for(WikiPage neighbour : page.getNeighbours()) {
                System.out.println(" - "+neighbour.getTitle());
            }
        }
    }

    public void displayPage(WikiPage page) {
        System.out.println("----oO "+page.getTitle()+" Oo----");
        System.out.println("URL: "+page.getLink().getLink());
        if(page.isRemoved()) {
            System.out.println("DELETED");
        }
        if(page.isRedirect()) {
            System.out.println("REDIRECT");
        }
        System.out.println(page.getDescription());
        System.out.println("Article Type: " + page.getArticleType());
        System.out.println("Unique Id: "+page.getUniqueId());
        System.out.println("Neighbours:");
        for(WikiPage neighbour : page.getNeighbours()) {
            System.out.println(" - "+neighbour.getTitle());
        }
    }

    public void displayShortestPaths(WikiPage page1, WikiPage page2, List<List<WikiPage>> shortestPaths) {
        if(shortestPaths.size()==0) {
            System.out.println("No path exists from "+page1.getTitle()+" to "+page2.getTitle());
            return;
        }

        System.out.println("Displaying Paths from "+page1.getTitle()+" to "+page2.getTitle());
        for(List<WikiPage> path : shortestPaths) {
            StringBuilder pathDisplay = new StringBuilder("[");
            int count = 0;
            for(WikiPage page : path) {
                pathDisplay.append(page.getTitle());
                if(count < path.size()-1) {
                    pathDisplay.append(" -> ");
                }
                count++;
            }
            pathDisplay.append("]");
            System.out.println(pathDisplay);
        }
    }
}
