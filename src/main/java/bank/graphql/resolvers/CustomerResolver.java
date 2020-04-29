package bank.graphql.resolvers;


import bank.graphql.models.Customer;
import bank.graphql.models.Rating;
import bank.graphql.repositories.ShopRepository;
import graphql.kickstart.tools.GraphQLResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerResolver implements GraphQLResolver<Customer> {

	@Autowired
	private ShopRepository shopRepository;

	// retuns the ratings of this customer
	public List<Rating> ratings(Customer c) {
		return shopRepository.getRatingsForCustomer(c);
	}
}
