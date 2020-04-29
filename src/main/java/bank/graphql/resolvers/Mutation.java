package bank.graphql.resolvers;


import bank.graphql.models.Product;
import bank.graphql.models.Rating;
import bank.graphql.repositories.ShopRepository;
import graphql.kickstart.tools.GraphQLMutationResolver;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Mutation implements GraphQLMutationResolver {

	@Autowired
	private ShopRepository shopRepository;

	public Product createProduct(@NotNull ProductInput product) {
		return shopRepository.createProduct(product.getTitle(), product.getDescription(), product.getImageUrl());
	}

	public Product updateProduct(String id, ProductInput productInput) {
		Optional<Product> op = shopRepository.getProductById(id);
		if (op.isPresent()) {
			Product p = op.get();
			p.setDescription(productInput.getDescription());
			p.setImageUrl(productInput.getImageUrl());
			p.setTitle(productInput.getTitle());
			return p;
		}
		return null;
	}

	public Rating rateProduct(String productId, String customerId, int score, String comment) {
		return shopRepository.rateProduct(productId, customerId, score, comment);
	}
}
