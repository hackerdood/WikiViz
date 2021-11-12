import edu.uci.ics.jung.algorithms.layout.*; // for graph visualization layouts
import edu.uci.ics.jung.algorithms.layout.util.*;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.renderers.*;
import edu.uci.ics.jung.visualization.decorators.*;
import edu.uci.ics.jung.visualization.control.*;
import javax.swing.JFrame; // for graph visualization
import java.awt.Dimension;
import javax.swing.JComponent.AccessibleJComponent;
import java.awt.Component.*;
import java.awt.*;
import java.util.*; // for HashMap and probably other things
import java.io.*; // for scanner 
import java.net.*;
import org.jsoup.Jsoup;  
import org.jsoup.nodes.Document;  


public class WikiPageGraph {

   public static double minRelevance = 0.01;
   public static int maxRelated = 15;
   public static final int SQUARESIZE = 750;
   public static int layers = 3;
   public static boolean fRLayout = true;
   public static boolean kKLayout = false;
   public static boolean circleLayout = false;
   public static boolean springLayout = false;
   
   public static void main (String [] args) throws Exception {
      // gets user inputs
      String url = getUrlFromUser();
      WikiPage w = new WikiPage(url);
      Scanner sc = new Scanner(System.in); 
      System.out.println("Would you like to edit more detailed parameters? (input \"true\" or \"false\")");
      if (sc.nextBoolean()) {
         System.out.println("How many layers deep? ");
         layers = sc.nextInt();
         System.out.println("Minimum relevance? Default is 0.01, the higher the percent, the fewer vertices there \nwill be (input a double between 0.001 and 0.04)");
         minRelevance = sc.nextDouble();
         System.out.println("Max related vertices for each vertex? Default is 15, be aware that high max related and \na low minimum relevance will cause so many vertices to be created the graph may crash (input an int)");
         maxRelated = sc.nextInt();
         System.out.println("Print a graph using the Fruchterman-Reingold force-directed algorithm for vertex layout? \nThis is the best alogrithm in almost every case (input \"true\" or \"false\")");
         fRLayout = sc.nextBoolean();
         System.out.println("Print a graph using the Kamada-Kawai algorithm for vertex layout? KKLayout creates better \nlarge graphs than FRLayout but takes a long time to iterate when the graph is \nbig (input \"true\" or \"false\")");
         kKLayout = sc.nextBoolean();
         System.out.println("Print a graph using a Layout implementation that positions vertices equally spaced on a \nregular circle? CircleLayout is kinda neat but not good for this type of data (input \"true\" or \"false\")");
         circleLayout = sc.nextBoolean();
         System.out.println("Print a graph using SpringLayout? SpringLayout is weirdly good except for the fact that \nit's a jello simulator (input \"true\" or \"false\")");
         springLayout = sc.nextBoolean();
      }
      double startTime = System.nanoTime();
      
      // builds the graph
      Graph g = new DirectedSparseGraph<WikiPage, Wrapper>();
      createLayer(w, layers - 1, g);
      System.out.println(g);
      
      // visualizes the graph
      if (fRLayout)
         visualizeGraph(new FRLayout(g));
      if (kKLayout)
         visualizeGraph(new KKLayout(g));
      if (circleLayout) 
         visualizeGraph(new CircleLayout(g));
      if (springLayout)   
         visualizeGraph(new SpringLayout(g));
      double endTime = System.nanoTime();
      double runTime = endTime - startTime;
      System.out.println("Time taken to run (seconds) " + (runTime / 1000000000.0));
      //visualizeGraph(two);
   }
   
   private static void createLayer(WikiPage central, int layer, Graph g) throws Exception {     
      g.addVertex(central);
      HashMap<WikiPage, Integer> related = central.getRelated(minRelevance, maxRelated);
      
      //For testing:
      System.out.println("Currently searching: " + central.toString());
      System.out.println("Layer: " + layer + "\n");
      
            
      // adds each new vertex to the graph
      for (WikiPage vertex : related.keySet()) {
         g.addVertex(vertex);
         int n = related.get(vertex);
         
         //Every edge needs a "different" frequency, use a wrapper
         Wrapper dumbStuff = new Wrapper (n);
         g.addEdge(dumbStuff, central, vertex);
      }    
      
      //Base case of recursion
      if (layer <= 1)
         return;
      
      //Recursive portion
      for (WikiPage w: related.keySet()) {
         createLayer(w, layer - 1, g);
      }
   }
   
   //Takes a graph and visualizes it using Jung and built in Jframe methods
   private static void visualizeGraph (Layout l) {
      // FRLayout iterates pretty quickly and creates decent graphs, especially when the number of layers is low
      // KKLayout creates better big graphs than FRLayout but takes a long time to iterate when the graph is big
      // CircleLayout is kinda neat but not good for this type of data
      // ISOMLayout is almost great but has some weird vertex spacing issues
      // DAGLayout threw a java.lang.StackOverflowError and I dont know why
      // SpringLayout is weirdly good except for the fact that it's a jello simulator
      //Layout l = new FRLayout2(g);
      VisualizationViewer vv = new VisualizationViewer(l, new Dimension (SQUARESIZE, SQUARESIZE) );
      vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
      vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());
      
      // sets up basic mouse control
      DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
      gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
      vv.setGraphMouse(gm);
      
      // adds key listener
      // change to translate (pan) mode if the user types a “t” and to picking mode if the user types a “p”
      vv.addKeyListener(gm.getModeKeyListener());
   
      
      JFrame jf = new JFrame(l.toString());
      //When the window closes, the run ends
      jf.setSize(SQUARESIZE,SQUARESIZE);
      jf.getContentPane().add(vv);
      jf.setVisible(true);
      jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   
   }
   
   //take the wikipedia url and return a Jsoup document
   public static Document getDocFromURL (String url) throws Exception{
      //Fixes the security issue. We dont know how. 
      SSLFix.execute();
      System.out.println("Url being tried: " + url);
      URL wiki = new URL(url);
      BufferedReader in = new BufferedReader(new InputStreamReader(wiki.openStream()));
      String inputLine;
      //res is the raw HTML 
      String res = "";
      while ((inputLine = in.readLine()) != null)
         res += inputLine + "\n";
      in.close();
      Document doc = Jsoup.parse(res); 
      
      //with Jsoup
      //Document doc = Jsoup.connect(url).get();
   
     
      return doc;
   }
   
   public static String getUrlFromUser () {
      Scanner scan = new Scanner (System.in);
      System.out.println("Input your wikipedia link");
      String url = scan.next();
      return url;
   }
   
}


class Wrapper {
   int frequency;
   
   public Wrapper (int freq) {
      this.frequency = freq;
   }

   public String toString () {
      return String.valueOf(this.frequency);
   }
}