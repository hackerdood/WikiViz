import java.util.*; // for Hashmap
import org.jsoup.Jsoup; // for Jsoup
import org.jsoup.nodes.Document; // for Document
import org.jsoup.select.Elements; // for Elements
import org.jsoup.nodes.Element; // for Element

public class WikiPage {
   
   private String fullLink;
   private String title;
   private Document fullDocument;
   
   public WikiPage (String fullLink) {
      this.fullLink = fullLink;
      this.title = "NO TITLE"; 
   }

   public WikiPage (String partialLink, String title) {
      this.fullLink = "https://en.wikipedia.org" + partialLink;
      this.title = title;
   }
   
   private Document getGoodDoc (Document d) throws Exception {
      Element bodyContent = d.getElementById("mw-content-text");
      int indexOfReferences = bodyContent.toString().indexOf("<h2><span class=\"mw-headline\" id=\"References\">References</span>");
      int indexOfCitations = bodyContent.toString().indexOf("<span class=\"mw-headline\" id=\"Citations\">Citations</span>");
      int lowerIndex;
      if (indexOfCitations < 0) {
         if (indexOfReferences < 0) {
            lowerIndex = bodyContent.toString().length();
         } else {
            lowerIndex = indexOfReferences;
         }
      } else if (indexOfReferences < 0) {
         lowerIndex = indexOfCitations;
      } else {
         lowerIndex = (indexOfReferences > indexOfCitations) ? indexOfCitations : indexOfReferences;
      }
      String goodStuff = bodyContent.toString().substring(0, lowerIndex);
      Document d2 = Jsoup.parse(goodStuff, this.fullLink);
      return d2;   
   }
   
   public HashMap <WikiPage, Integer> getRelated (double minRelevance, int maxRelated) throws Exception {
      // get Doc of article
      Document d = WikiPageGraph.getDocFromURL(this.fullLink);
      this.fullDocument = d;
      String tempTitle = d.title();
      
      Document doc = getGoodDoc(d);
      
      
      // get raw text, for searching 
      String allText = getText(doc);
      //System.out.println(allText); //FOR TESTING
      
      HashMap<WikiPage, Integer> absFreq = buildMap (doc);
      countFreqs(absFreq, allText);
      //System.out.println(absFreq); //FOR TESTING
   
      
      
      HashMap<WikiPage, Integer> relevantPages = new HashMap<WikiPage, Integer> ();
      int total = 0;
      for (WikiPage w : absFreq.keySet()) {
         total += absFreq.get(w);
      }
      //System.out.println(total); //FOR TESTING
      for (WikiPage w : absFreq.keySet()) {
         double weightedRelevance = (double) absFreq.get(w) / total;
         if (weightedRelevance > minRelevance) {
            int cleanedUpRelevance = (int) (weightedRelevance * 1000);
            relevantPages.put(w, cleanedUpRelevance);
         }
      }
      
      //reduces number of related articles down to maxRelated
      if (relevantPages.size() > maxRelated) {
         HashMap<WikiPage, Integer> correctRelevantPages = new HashMap<WikiPage, Integer>();
         while (correctRelevantPages.size() < maxRelated) {
            WikiPage highestRelatedPage = null;
            int highestFreq = 0;
            for (WikiPage vertex : relevantPages.keySet()) {
               if (correctRelevantPages.containsKey(vertex))
                  continue;
               int freq = relevantPages.get(vertex);
               if (freq > highestFreq) {
                  highestRelatedPage = vertex;
               }
            }
            correctRelevantPages.put(highestRelatedPage, relevantPages.get(highestRelatedPage));
         }
         relevantPages = correctRelevantPages;
      }
   
         
      // relevant WikiPages are put into a new HashMap <WikiPage, Double> "relevantPages"
      // return relevantPages
      return relevantPages;
   }
   
   //Takes the document and returns the raw text, to search for frequencies
   private String getText (Document d) {     
      //Element paragraphs = d.getElementById("bodyContent");//.getElementsByTag("p");//.removeClass("references").removeClass("reflist").removeClass("navbox");
      String res = d.text().toLowerCase();
      return res;
   } 
   
   private HashMap<WikiPage, Integer> buildMap (Document d) {
      //get element objects for each link in the article
      Elements links = d.select("a[href]");
      
      HashMap<WikiPage, Integer> absFreq = new HashMap<WikiPage, Integer> ();
      //These actions are performed for every link
      for (Element link : links) {
         String rawLink = link.toString();
         String displayText = link.text();
         if (displayText.equals("")) {
            continue;
         }
         //If the link is an internal citation, ignore it 
         if (displayText.charAt(0) == '[') {
            continue;
         }
         
         //get the title of the article 
         int titleStart = rawLink.indexOf("title=\"") + 7;
         int titleEnd = rawLink.indexOf("\"", titleStart);
         String title = rawLink.substring(titleStart, titleEnd);
         
         //get the link for the article
         int start = rawLink.indexOf("\"") + 1;
         int end = rawLink.indexOf("\"", start + 1);
         String partialLink = rawLink.substring(start, end);
         if (partialLink.indexOf("/wiki/") != 0) {
            continue;
         }
         
         //If a link appears only once and the display is different from title, its frequency will be 0. 
         //This line attempts to fix that issue 
         int n = (displayText.equals(title))? 0 : 1;
         //create article object, set frequency to 0 to start 
         absFreq.put(new WikiPage(partialLink, title), n);
      }
      return absFreq;
   }
   
   public void countFreqs (HashMap <WikiPage, Integer> absFreq, String allText) {
      //Gets the strings of the linked article titles for easy searching 
      HashMap <String, WikiPage> tableOfContents = new HashMap <String, WikiPage> ();
      int longestKey = 0;
      for (WikiPage w : absFreq.keySet()) {
         String title = w.getTitle().toLowerCase();
         if (title.length() > longestKey) {
            longestKey = title.length();
         }
         tableOfContents.put(title, w);
      }
      
      //Search through the text and set the value of absFreq to the occurrence of each article title     
      for (int i = 0; i < allText.length() - 1; i++) {
         if (i > 1 && allText.charAt(i-1) != ' ') {
            continue;
         }   
         for (int j = i; j <= i + longestKey && j < allText.length(); j++) {
            String candidate = allText.substring(i, j);
            if (tableOfContents.containsKey(candidate)) {
               WikiPage w = tableOfContents.get(candidate);
               int n = absFreq.get(w);
               absFreq.put(w, n + 1);
            }
         }
      
      }
   
   }
   
   
   
   public String getTitle() {
      return title;
   }
   
   public String toString () {
      return /*"Link: " + this.fullLink + " Title:" + */this.title;
   }
   
   public boolean equals(Object o) {
      if (o == null)
         return false;
   
      if (o == this)
         return true;
         
      if (!(o instanceof WikiPage))
         return false;
      
      WikiPage other = (WikiPage) o;
      if (this.getTitle().equals(other.getTitle()))
         return true;
         
      return false;
   
   }
   
   //Ensures articles with the same title have the same hash
   public int hashCode() {
      int hash = 0;
      if (this.title != null) {
         hash = this.title.hashCode();
      }
        //by convention, multiply result by 31
      final int prime = 31;
      return prime + hash;
   }
}