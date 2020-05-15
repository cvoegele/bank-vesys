package bank.akka;

import akka.actor.AbstractActor;
import bank.BankDriver2;
import bank.command.Command;
import bank.command.LoginCommand;
import bank.command.LogoutCommand;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BankClientActor extends AbstractActor {

    private List<BankDriver2.UpdateHandler> listeners;

    public BankClientActor(List<BankDriver2.UpdateHandler> listeners) {
        this.listeners = listeners;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(LoginCommand.class, o -> System.out.println("successfully logged in"))
                .match(LogoutCommand.class,  o -> {
                    System.out.println("logging out");
                    getContext().system().terminate();
                })
                .match(Command.class, command -> {
                    String number = (String) command.parameters[0];
                    if (command.parameters.length > 1){
                        for (BankDriver2.UpdateHandler listener : listeners) {
                            listener.accountChanged((String) command.parameters[1]);
                        }
                    }

                    for (BankDriver2.UpdateHandler listener : listeners) {
                        listener.accountChanged(number);
                    }

                }).matchAny(msg -> {
                    System.out.println("received command for: " + msg);
                    getSender().tell(msg, getSelf());
                }).build();
    }
}
