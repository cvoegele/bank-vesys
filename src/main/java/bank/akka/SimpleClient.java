package bank.akka;

import akka.actor.*;
import bank.network.UDP.BankPackage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

//simple test client for the akka bank server
public class SimpleClient {
    public static void main(String[] args) {
        Config config = ConfigFactory
                .parseString("akka.remote.artery.canonical.port=0")
                .withFallback(ConfigFactory.load().getConfig("BankConfig"));
        System.out.println(config);

        ActorSystem system = ActorSystem.create("BankApplication", config);
        ActorRef client = system.actorOf(Props.create(SimpleClientActor.class), "BankActor");
        System.out.println("Started Bank Client");

        ActorSelection serverActor = system.actorSelection("akka://BankApplication@127.0.0.1:2552/user/BankServer");

        BankPackage request = new BankPackage("ass", "Hans", null, 0.0);

        serverActor.tell(request, client);

    }

    static class SimpleClientActor extends AbstractActor {

        @Override
        public Receive createReceive() {
            return receiveBuilder().matchAny(System.out::println).build();
        }
    }
}
