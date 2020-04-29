package bank.graphql.resolvers;


import bank.graphql.models.Product;
import bank.graphql.repositories.ShopRepository;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

@Component
public class Query implements GraphQLQueryResolver {

	@Autowired
	private ShopRepository shopRepository;

	public Collection<Product> products() {
		return shopRepository.getAllProducts();
	}

	public Optional<Product> product(String id) {
		return shopRepository.getProductById(id);
	}
}
