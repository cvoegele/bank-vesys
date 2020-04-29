package bank.graphql.resolvers;


import bank.graphql.models.Product;
import bank.graphql.models.Rating;
import bank.graphql.repositories.ShopRepository;
import graphql.kickstart.tools.GraphQLResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.OptionalDouble;

@Component
public class ProductResolver implements GraphQLResolver<Product> {

	@Autowired
	private ShopRepository shopRepository;

	public List<Rating> ratings(Product p) {
		return shopRepository.getRatingsForProduct(p);
	}

	public OptionalDouble averageRatingScore(Product p) {
		return shopRepository.getRatingsForProduct(p).stream().mapToInt(r -> r.getScore()).average();
	}
}
