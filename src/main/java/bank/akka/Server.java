package bank.akka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import bank.Account;
import bank.InactiveException;
import bank.OverdrawException;
import bank.command.Command;
import bank.command.CommandType;
import bank.command.LoginCommand;
import bank.command.LogoutCommand;
import bank.local.Driver;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.util.*;


public class Server {

    protected static Driver.Bank bank;

    public static void main(String[] args) {
        Config config = ConfigFactory.load().getConfig("BankConfig");
        ActorSystem system = ActorSystem.create("BankApplication", config);
        system.actorOf(Props.create(BankActor.class, system), "BankServer");
        System.out.println("Started Bank Application");
        bank = new Driver.Bank();
    }

    //central bank actor --> takes all requests and moves them to corresponding Account Actors
    static class BankActor extends AbstractActor {

        private final HashMap<String, ActorRef> accountRefs = new HashMap<>();
        protected static final Set<ActorRef> clients = new HashSet<>();

        private final ActorSystem system;


        public BankActor(ActorSystem system) {
            this.system = system;
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(LoginCommand.class, o -> {
                        clients.add(getSender());
                        getSender().tell(new LoginCommand(), getSelf());
                    }).match(LogoutCommand.class, o -> {
                        clients.remove(getSender());
                        getSender().tell(new LogoutCommand(), getSelf());
                    })
                    .match(Command.class, this::processCommand)
                    .matchAny(msg -> {
                        System.out.println("Unhandled Message Received");
                        unhandled(msg);
                    }).build();
        }

        public void processCommand(Command request) {
            //create new Actor for new account
            ActorRef sender = getSender();

            switch (request.command) {
                case CREATEACCOUNT:
                    //setup actual account
                    String name = (String) request.parameters[0];
                    String number = bank.createAccount(name);

                    //setup new account ActorRef
                    ActorRef accountActor = system.actorOf(Props.create(AccountActor.class, number), "AccountActor" + number);
                    accountRefs.put(number, accountActor);

                    //send confirmation to all client including the one that send the request
                    Command information = new Command(CommandType.RESPONSE, new String[]{number});
                    informAllClients(information);
                    sender.tell(information, getSelf());

                    break;
                case CLOSEACCOUNT:
                    //try closing actual account
                    number = (String) request.parameters[0];
                    boolean worked = bank.closeAccount(number);
                    //inform sender of request of success
                    Command response = new Command(CommandType.RESPONSE, new Object[]{worked});
                    getSender().tell(response, getSelf());

                    //inform other clients if there was change
                    if (worked) {
                        information = new Command(CommandType.RESPONSE, new String[]{number});
                        informAllClients(information);
                    }

                    break;
                case GETACCOUNTS:
                    //get all account from actual bank
                    Set<String> numbers = bank.getAccountNumbers();

                    //send response to client that asked
                    response = new Command(CommandType.RESPONSE, numbers.toArray());
                    getSender().tell(response, getSelf());
                    break;
                case TRANSFER:
                    System.out.println("received transfer");
                    String fromNumber = (String) request.parameters[0];
                    String toNumber = (String) request.parameters[1];
                    double amount = (double) request.parameters[2];

                    Account from = bank.getAccount(fromNumber);
                    Account to = bank.getAccount(toNumber);

                    try {

                        bank.transfer(from, to, amount);
                        response = new Command(CommandType.RESPONSE, new Object[]{true});

                    } catch (IOException | InactiveException | OverdrawException e) {
                        response = new Command(CommandType.RESPONSE, new Object[]{e});
                    }

                    //inform clients on changed amount
                    informAllClients(new Command(CommandType.RESPONSE, new Object[]{fromNumber, toNumber}));

                    //inform sender of success
                    getSender().tell(response, getSelf());
                    break;
                default:
                    //forward to AccountActor
                    number = (String) request.parameters[0];
                    ActorRef accountRef = accountRefs.get(number);
                    if (accountRef != null) {
                        accountRef.forward(request, getContext());
                    } else {
                        //inform client that this account is not existent
                        getSender().tell(new Command(CommandType.RESPONSE, new Object[]{null}), getSelf());
                    }

                    break;
            }
        }

        private void informAllClients(Command information) {
            for (ActorRef client : clients) {
                //inform all other clients
                if (!client.equals(getSender())) {
                    //System.out.println("informing client " + client);
                    client.tell(information, getSelf());
                }
            }
        }
    }


}
