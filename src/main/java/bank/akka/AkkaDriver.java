package bank.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import bank.*;
import bank.command.Command;
import bank.command.CommandBank;
import bank.command.LoginCommand;
import bank.command.LogoutCommand;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AkkaDriver implements BankDriver2 {

    UpdateHandler handler;
    private CommandBank bank;
    private final List<UpdateHandler> listeners
            = new CopyOnWriteArrayList<>();

    ActorSelection serverActor;
    ActorRef client;

    @Override
    public void registerUpdateHandler(UpdateHandler updateHandler)  {
        listeners.add(updateHandler);
    }

    @Override
    public void connect(String[] strings)  {
        Config config = ConfigFactory
                .parseString("akka.remote.artery.canonical.port=0")
                .withFallback(ConfigFactory.load().getConfig("BankConfig"));
        System.out.println(config);

        ActorSystem system = ActorSystem.create("BankApplication", config);
        client = system.actorOf(Props.create(BankClientActor.class, listeners), "BankActor");
        System.out.println("Started Bank Client");

        serverActor = system.actorSelection("akka://BankApplication@127.0.0.1:2552/user/BankServer");

        //login at server
        serverActor.tell(new LoginCommand(), client);

        bank = new CommandBank(msg -> {
            try {
                Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
                System.out.println("asking " + serverActor);
                Future<Object> res = Patterns.ask(serverActor, msg, timeout);
                Object result = Await.result(res, timeout.duration());
                return (Command) result;
            } catch (InterruptedException | TimeoutException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public void disconnect() {
        //logout at server
        serverActor.tell(new LogoutCommand(), client);
        System.out.println("stop");
    }

    @Override
    public bank.Bank getBank() {
        return bank;
    }

}
