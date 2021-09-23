import com.rabbitmq.client.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main
{
    private static String[] queueNames;
    private static Process[] processes;
    private static String cmd;

    private static Connection connection;
    private static Channel channel;

    public static void main(String[] args) throws IOException, InterruptedException
    {
        if (args.length == 0)
        {
            System.err.println(ANSI_RED + "Wrong arguments:\n" +
                    "\tmissed osm files" + ANSI_RESET);
            return;
        }

        init(args);
        try
        {
            createSubprocesses(args);

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();

            queueDeclare(channel);

            Scanner s = new Scanner(System.in);
            System.out.println(ANSI_GREEN + "Started:");
            System.out.println("Enter an empty string to show all streets");

            while (true)
            {
                cmd = s.nextLine();
                if (cmd == null || cmd.equals("exit"))
                    break;

                basicPublish(channel, cmd);
                consume(channel);
            }
        }
        catch (Exception e)
        {
            System.err.println(ANSI_RED + "Error: " + ANSI_RED);
            e.printStackTrace();
            basicPublish(channel, "exit");
            queueDelete(channel);
            if (connection != null)
                connection.close();
            waitFor();
        }
        finally
        {
            basicPublish(channel, "exit");
            queueDelete(channel);
            if (connection != null)
                connection.close();
            waitFor();
        }
    }

    private static void init(String[] args)
    {
        queueNames = new String[args.length];

        for (int i = 0; i < args.length; i++)
        {
            String tmp = args[i].substring(0, args[i].indexOf(".osm"));
            queueNames[i] = tmp;
        }
    }

    private static void queueDeclare(Channel channel) throws IOException
    {
        for (String queue : queueNames)
        {
            channel.queueDeclare(queue + "Queue", true, false, false, null);
            channel.queueDeclare(queue + "Streets", true, false, false, null);
        }
    }

    private static void basicPublish(Channel channel, String cmd) throws IOException
    {
        for (String queueName : queueNames)
            channel.basicPublish("", queueName + "Queue", MessageProperties.PERSISTENT_TEXT_PLAIN,
                                    cmd.getBytes(StandardCharsets.UTF_8));
    }

    private static void consume(Channel channel) throws IOException
    {
        for (String queueName : queueNames)
        {
            Consumer consumer = new DefaultConsumer(channel)
            {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException
                {
                    String msg = new String(body, StandardCharsets.UTF_8);

                    String cityName = queueName.substring(queueName.lastIndexOf("/") + 1);
                    cityName = cityName.substring(0, 1).toUpperCase(Locale.ROOT) + cityName.substring(1);
                    System.out.print(ANSI_PURPLE + cityName + " has ");


                    if (msg.isEmpty() || msg.equals(" "))
                        System.out.println("no streets beginning with \"" + cmd + "\"" + ANSI_RESET);
                    else
                    {
                        String[] tmp = msg.split(" ");
                        String streets = String.join(", ", tmp);
                        if (cmd.equals(""))
                            System.out.println(tmp.length + " streets:");
                        else
                            System.out.println(tmp.length + " street" + (tmp.length == 1 ? "" : "s") +
                                                " beginning with \"" + cmd + "\":");

                        System.out.println(ANSI_CYAN + streets + ANSI_RESET + "");
                    }
                }
            };
            channel.basicConsume(queueName + "Streets", true, consumer);
        }
    }

    private static void queueDelete(Channel channel) throws IOException
    {
        for (String queue : queueNames)
        {
            channel.queueDelete(queue + "Queue");
            channel.queueDelete(queue + "Streets");
        }
    }

    private static void createSubprocesses(String[] args) throws IOException
    {
        String arguments = "-cp out/production/RabbitMQ/.:./lib/rabbitmq.jar Worker ";
        Runtime run = Runtime.getRuntime();
        processes = new Process[args.length];

        for (int i = 0; i < args.length; i++)
            processes[i] = run.exec("java " + arguments + args[i]);
    }

    private static void waitFor() throws InterruptedException
    {
        for (Process process : processes)
            process.waitFor();
    }

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
}
