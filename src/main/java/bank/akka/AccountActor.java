package bank.akka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import bank.Account;
import bank.Bank;
import bank.InactiveException;
import bank.OverdrawException;
import bank.command.Command;
import bank.command.CommandType;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AccountActor extends AbstractActor {


    String accountNumber;

    public AccountActor(String accountNumber) {
       this.accountNumber = accountNumber;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.class, command -> {
                    Command response = processCommand(command);
                    getSender().tell(response, getSelf());
                })
                .matchAny(msg -> {
                    System.out.println("received command for: " + msg);
                    getSender().tell(msg, getSelf());
                }).build();
    }

    public Command processCommand(Command request) throws IOException {
        //basic response when nothing of the below ones worked or could be applied
        Command response = new Command(CommandType.RESPONSE, new Object[]{false});
        //System.out.println("account " + Server.bank.getAccount(accountNumber) + " balance " + Server.bank.getAccount(accountNumber).getBalance());
        switch (request.command) {

            case GETNUMBER:
                response = new Command(CommandType.RESPONSE, new Object[]{Server.bank.getAccount(accountNumber).getNumber()});
                break;
            case GETOWNER:
                response = new Command(CommandType.RESPONSE, new Object[]{Server.bank.getAccount(accountNumber).getOwner()});
                break;
            case GETBALANCE:
                response = new Command(CommandType.RESPONSE, new Object[]{Server.bank.getAccount(accountNumber).getBalance()});
                break;
            case ACTIVE:
                response = new Command(CommandType.RESPONSE, new Object[]{Server.bank.getAccount(accountNumber).isActive()});
                break;
            case DEPOSIT:
                double amount = (double) request.parameters[1];
                try {
                    Server.bank.getAccount(accountNumber).deposit(amount);
                    response = new Command(CommandType.RESPONSE, new Object[]{true});

                    //inform other clients
                    Command information = new Command(CommandType.RESPONSE, new Object[]{Server.bank.getAccount(accountNumber).getNumber()});
                    informAllClients(information);
                } catch (InactiveException e) {
                    e.printStackTrace();
                }
                break;
            case WITHDRAW:
                amount = (double) request.parameters[1];
                try {
                    //withdraw money
                    Server.bank.getAccount(accountNumber).withdraw(amount);
                    //inform client that requested this operation
                    response = new Command(CommandType.RESPONSE, new Object[]{true});

                    //inform other clients
                    Command information = new Command(CommandType.RESPONSE, new Object[]{Server.bank.getAccount(accountNumber).getNumber()});
                    informAllClients(information);
                } catch (InactiveException | OverdrawException e) {
                    e.printStackTrace();
                }
                break;

        }
        return response;
    }

    private void informAllClients(Command information) {
        for (ActorRef client : Server.BankActor.clients) {
            //inform all other clients
            if (!client.equals(getSender())) {
                System.out.println("informing client " + client);
                client.tell(information, getSelf());
            }
        }
    }
}
