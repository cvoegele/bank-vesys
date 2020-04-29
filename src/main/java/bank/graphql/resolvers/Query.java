package bank.graphql.resolvers;


import bank.graphql.models.Account;
import bank.graphql.repositories.BankRepository;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class Query implements GraphQLQueryResolver {

    @Autowired
    private BankRepository bankRepository;

    public Collection<Account> accounts() {
        var result = new ArrayList<Account>();
        var accStrings = bankRepository.getAccountNumbers();
        for (var acc : accStrings) {
            result.add(new Account(bankRepository.getAccount(acc)));
        }
        return result;
    }

    public Account account(@NotNull String number) {
        var account = (bankRepository.getAccount(number));
        if (account == null) return null;
        return new Account(account);
    }
}
