import com.rabbitmq.client.*;

import java.util.Scanner;

public class Main
{
    static ConnectionFactory factory = new ConnectionFactory();

    public static void main(String[] args)
    {
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel())
        {
            System.out.println("Start");
            Scanner s = new Scanner(System.in);
            while (true)
            {
                String cmd = s.nextLine();
                if (cmd.equals("finish"))
                    break;
                channel.basicPublish("", );

            }
        }
        catch (Exception e)
        {
            System.err.println("!Error: ");
            e.printStackTrace();
        }
    }
}
