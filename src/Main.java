import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;

import util.mapUtil;

public class Main
{
    static Map<String, String[]> streets = new HashMap<>();
    static String[] queueNames;
    static ConnectionFactory factory = new ConnectionFactory();
    static Process[] processes;
    static int finalI;
    static String cmd;

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException
    {
        if (args == null)
        {
            return;
        }

        init(args);
        createWorkers(args);

        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        try
        {
            queueDeclare(channel);
            System.out.println("Start");
            Scanner s = new Scanner(System.in);

            while (true)
            {
                cmd = s.nextLine();
                if (cmd == null || cmd.equals("exit"))
                    break;

                basicPublish(channel, cmd);
                consume(channel);
                printStreets();
                streets.clear();
            }

            basicPublish(channel, "exit");
            queueDelete(channel);
            connection.close();
        }
        catch (Exception e)
        {
            System.err.println("Error: ");
            e.printStackTrace();
            basicPublish(channel, "exit");
            queueDelete(channel);
            connection.close();
        }

        //waitForWorkers();
    }

    protected static void printStreets()
    {
        System.out.println(ANSI_BLUE + "Streets:" + ANSI_RESET);
        for(String key : streets.keySet())
        {
            System.out.println(ANSI_PURPLE + key + ANSI_RESET);
            for (String city : streets.get(key))
                System.out.println(city);
        }
    }

    protected static void init(String[] args)
    {
        queueNames = new String[args.length];

        for (int i = 0; i < args.length; i++)
        {
            String tmp = args[i].substring(0, args[i].indexOf(".osm"));
            queueNames[i] = tmp;
        }
    }

    protected static void createWorkers(String[] args) throws IOException
    {
        processes = new Process[args.length];

        for (int i = 0; i < args.length; i++)
        {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("java", "-cp", "\".:./../lib/rabbitmq.jar\"", "Worker", args[i]);
            processes[i] = processBuilder.start();
        }
    }

    protected static void waitForWorkers() throws InterruptedException
    {
        for (Process p : processes)
            p.wait();
    }

    protected static void queueDeclare(Channel channel) throws IOException
    {
        for (String queue : queueNames)
        {
            channel.queueDeclare(queue + "Queue", false, false, false, null);
            channel.queueDeclare(queue + "Streets", false, false, false, null);
        }
    }

    protected static void queueDelete(Channel channel) throws IOException
    {
        for (String queue : queueNames)
        {
            channel.queueDelete(queue + "Queue");
            channel.queueDelete(queue + "Streets");
        }
    }

    protected static void basicPublish(Channel channel, String cmd) throws IOException
    {
        for (String queueName : queueNames)
            channel.basicPublish("", queueName + "Queue", null, cmd.getBytes(StandardCharsets.UTF_8));
    }

    protected static void consume(Channel channel) throws IOException, InterruptedException
    {
        for (int i = 0; i < queueNames.length; i++)
        {
            finalI = i;
            DeliverCallback deliverCallback = (consumerTag, delivery) ->
            {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                streets.put(queueNames[finalI], message.split(" "));
            };
            channel.basicConsume(queueNames[finalI] + "Streets", true, deliverCallback, consumerTag -> {});
        }
    }

    /*protected static void printStreets()
    {
        streets = mapUtil.sortMapByValueSize(streets);

        int columnCount = streets.size();
        int lines = columnCount / 3;

        Vector<String> cities = new Vector<>(streets.keySet());
        Vector<String[]> streetsOfCities = (Vector<String[]>) streets.values();

        int[] amountOfColumnsInLine = new int[lines];
        for (int i = 0; i < lines - 1; i++)
            amountOfColumnsInLine[i] = 3;
        amountOfColumnsInLine[lines - 1] = columnCount % 3;

        for (int i = 0; i < lines; i++)
        {
            for (int j = 0; j < amountOfColumnsInLine[i]; j++)
            {
                System.out.print(ANSI_BLUE + cities.elementAt(i * 3 + j) + ANSI_RESET + "\t");
                for (String street : streetsOfCities.elementAt(i * 3 + j))
                    System.out.print(street + "\t");
            }
            System.out.println();
        }
    }*/

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
}
