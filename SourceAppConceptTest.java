import org.jsoup.Jsoup; // for Jsoup
import org.jsoup.nodes.Document; // for Document
import org.jsoup.select.Elements; // for Elements
import org.jsoup.nodes.Element; // for Element

public class SourceAppConceptTest {

   public static void main(String[] args) throws Exception {
      //Document d = WikiPageGraph.getDocFromURL("https://ps.seattleschools.org/public/home.html");
      Document doc = Jsoup.connect("https://ps.seattleschools.org/public/home.html")
            .data("userName", "your username")
            .data("password", "your password")
            .post();
      System.out.println(doc.text());

   }
   
}