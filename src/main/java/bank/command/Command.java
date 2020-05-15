package bank.command;

import java.io.Serializable;

public class Command implements Serializable {
    public final CommandType command;
    public final Object[] parameters;

    public Command(CommandType command, Object[] parameters) {
        this.command = command;
        this.parameters = parameters;
    }
}

