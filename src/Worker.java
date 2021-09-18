import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
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
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class Worker
{
    static Set<String> streets = new HashSet<>();
    static String cmd;

    public static void main(String[] args)
    {
        try
        {
            if (args == null)
            {
                System.err.println("Wrong arguments");
                return;
            }

            parseOsm(args[1]);
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            String queueName = args[1].substring(0, args[1].indexOf(".osm"));
            channel.queueDeclare(queueName + "Queue", false, false, false, null);
            channel.queueDeclare(queueName + "Streets", false, false, false, null);

            while(true)
            {
                DeliverCallback deliverCallback = (consumerTag, delivery) ->
                {
                    cmd = new String(delivery.getBody(), StandardCharsets.UTF_8);
                };
                channel.basicConsume(queueName + "Queue", true, deliverCallback, consumerTag -> {});

                if (cmd.equals("exit"))
                    break;

                channel.basicPublish("", queueName + "Streets", null, String.join(" ", streets).getBytes(StandardCharsets.UTF_8));
            }
        }
        catch (Exception e)
        {
            System.err.println("Error!");
            e.printStackTrace();
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
