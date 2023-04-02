# Wikipedia Game

The wikipedia game project tries to save the famous "wikiracing" game. [Wikiracing](https://en.wikipedia.org/wiki/Wikiracing) is a game where multiple players try to compete to navigate through the website. Players start off at one random wikipedia page and only using internal links try to navigate to a different internal page.

![Image showing the wikipedia game as a connected graph](wikipedia_graph.webp)

The program works by crawling wikipedia and fetching pages. It then grabs the links and navigates to the next page, the output is put into a connected graph. One can then calculate the shortest path by running BFS.  

## Building

The project and its dependencies are built and managed by maven.

```sh
mvn clean install
```

## Running 

One can run the project by using `java` command on the built output. The output should be located in the `target` folder. One can then interact with the program using the command line.

```sh
java -jar <out>.jar
```

One should run the cli project's jar.

The commands are as follows
  - crawler -> responsible for start and stopping the crawler
  - exit -> exit the command line
  - wiki -> navigate wikipedia. 
