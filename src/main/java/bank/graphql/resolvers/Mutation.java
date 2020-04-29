package bank.graphql.resolvers;

import bank.InactiveException;
import bank.OverdrawException;
import bank.graphql.models.Account;
import bank.graphql.repositories.BankRepository;
import graphql.kickstart.tools.GraphQLMutationResolver;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Mutation implements GraphQLMutationResolver {

    @Autowired
    private BankRepository bankRepository;

    public Account createAccount(@NotNull String owner) {
        var number = bankRepository.createAccount(owner);
        var account = bankRepository.getAccount(number);
        return new Account(account);
    }

    public boolean closeAccount(@NotNull String number) {
        return bankRepository.closeAccount(number);
    }

    public String deposit(@NotNull String number, float amount) {
        try {
            bankRepository.getAccount(number).deposit(amount);
            return "true";
        } catch (IOException | InactiveException e) {
            return e.getClass().getName();
        }
    }

    public String withdraw(@NotNull String number, float amount) {
        try {
            bankRepository.getAccount(number).withdraw(amount);
            return "true";
        } catch (IOException | InactiveException | OverdrawException e) {
            return e.getClass().getName();
        }
    }

    public String transfer(@NotNull String from, String to, float amount) {
        try {
            bankRepository.transfer(bankRepository.getAccount(from), bankRepository.getAccount(to), amount);
            return "true";
        } catch (IOException | InactiveException | OverdrawException e) {
            return e.getClass().getName();
        }
    }

}
