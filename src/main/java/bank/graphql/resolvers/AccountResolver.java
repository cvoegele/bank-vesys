package bank.graphql.resolvers;

import bank.graphql.models.Account;
import graphql.kickstart.tools.GraphQLResolver;
import org.springframework.stereotype.Component;

@Component
public class AccountResolver implements GraphQLResolver<Account> {
}
