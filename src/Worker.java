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
import java.util.Vector;
import java.util.concurrent.TimeoutException;

public class Worker
{
    static Set<String> streets = new HashSet<>();
    static String cmd;

    static ConnectionFactory factory;
    static Connection connection;
    static Channel channel;
    static String queueName;

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, TimeoutException
    {
        if (args == null)
        {
            System.err.println("Wrong arguments");
            return;
        }

        parseOsm(args[0]);
        queueName = args[0].substring(0, args[0].indexOf(".osm"));

        factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(queueName + "Queue", false, false, false, null);
        channel.queueDeclare(queueName + "Streets", false, false, false, null);

        DeliverCallback deliverCallback = (consumerTag, delivery) ->
        {
            cmd = new String(delivery.getBody(), StandardCharsets.UTF_8);
            byte[] msg = String.join(" ", getStreets(cmd)).getBytes(StandardCharsets.UTF_8);
            if (cmd.equals("exit"))
            {
                channel.queueDelete(queueName + "Queue");
                channel.queueDelete(queueName + "Streets");
                connection.close();
            }
            channel.basicPublish("", queueName + "Streets", null, msg);
        };
        channel.basicConsume(queueName + "Queue", true, deliverCallback, consumerTag -> {});
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

    public static Vector<String> getStreets(String prefix)
    {
        Vector<String> res = new Vector<>();

        for (String street : streets)
            if (street.startsWith(prefix))
                res.add(street);

        return res;
    }
}
