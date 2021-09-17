import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Worker
{
    static Set<String> streets = new HashSet<>();
    public static void main(String[] args)
    {
        try
        {
            parseOsm(args[1]);

        }
        catch (Exception e)
        {
            System.err.println("Error!");
            e.printStackTrace();
        }
        finally
        {

        }
    }

    public static void parseOsm(String filename) throws ParserConfigurationException, IOException, SAXException
    {
        File osmFile = new File(filename);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(osmFile);
        document.getDocumentElement().normalize();
        NodeList nodeList = document.getElementsByTagName("way");

        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element element = (Element) node;
                NodeList nodeList1 = element.getElementsByTagName("tag");
                for (int j = 0; j < nodeList1.getLength(); j++)
                {
                    Node node1 = nodeList1.item(j);
                    if (node1.getNodeType() == Node.ELEMENT_NODE)
                    {
                        Element element1 = (Element) node1;
                        if (element1.getAttribute("k").equals("highway"))
                            streets.add(element1.getAttribute("v"));
                    }
                }
            }
        }
    }
}
