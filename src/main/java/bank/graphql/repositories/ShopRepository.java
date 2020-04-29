package bank.graphql.repositories;


import bank.graphql.models.Customer;
import bank.graphql.models.Product;
import bank.graphql.models.Rating;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ShopRepository {
	private final AtomicInteger pid = new AtomicInteger(0);
	private final AtomicInteger cid = new AtomicInteger(0);
	
	private final Map<String, Customer> customers = new HashMap<>();
	private final Map<String, Product> products = new HashMap<>();
	
	private final Map<Customer, List<Rating>> cRatings = new HashMap<>();
	private final Map<Product, List<Rating>> pRatings = new HashMap<>();

	{
		createCustomer("Daniel");
		createCustomer("Dierk");
		createCustomer("Dominik");

		createProduct(
				"PHILIPPI Paco Brieföffner",
				"Wenn der Postmann zweimal klingelt",
				"https://static.digitecgalaxus.ch/Files/1/3/1/9/0/2/1/2/Unbenannt-121.jpeg"
		);
		createProduct(
				"Driade Nemo", 
				"Kunst- und Sitzobjekt zugleich: Der von Fabio Novembre für Driade designte Sessel eignet sich sowohl für drinnen als auch für draussen.",
				"https://static.digitecgalaxus.ch/Files/8/1/4/9/5/6/2/051002079_01_1.jpg"
		);
		createProduct(
				"Monkey Business Umbrella",
				"Das etwas andere Teesieb.",
				"https://static.digitecgalaxus.ch/Files/5/1/7/0/5/5/7/ot800_teeei_regenschirm4_k_z1.jpg"
		);
		createProduct(
				"Freezack Vibro Mouse", 
				"Die Vibro Mouse aus dem Hause Freezack ist ein vibrierender Spielspass für Ihren Liebling. Das weiche Material schützt Pfoten und Zähne. Die Batterien sind im Lieferumfang enthalten.",
				"https://static.digitecgalaxus.ch/Files/7/8/5/8/3/9/2/31665-freezack-vibro-mouse-orange-31665-p.jpg"
		);
		createProduct(
				"Siena Garden: Katzenabwehrgürtel",
				"Drahtgürtel zum Schutz von frei- und höhlenbrütenden Vögel, der individuell auf den Umfang des Baumes anpassbar ist.",
				"https://static.digitecgalaxus.ch/Files/8/2/7/1/0/8/5/4901591clr.jpg?fit=inside%7C828:414&output-format=progressive-jpeg"
		);

			
		rateProduct("1", "1", 1, "really bad");
		rateProduct("2", "1", 2, "well... If you don't need it it is ok");
		rateProduct("2", "2", 4, "great product");
		rateProduct("3", "1", 4, "I like this product");
		rateProduct("3", "2", 3, "what the hack is this????");
		rateProduct("3", "3", 5, "awsome!!!");
	}

	public Product createProduct(String title, String description, String url) {
		String key = "" + pid.incrementAndGet();
		Product p = new Product(key, title, description, url);
		products.put(key, p);
		return p;
	}
	
	public Customer createCustomer(String name) {
		String key = "" + cid.incrementAndGet();
		Customer c = new Customer(key, name);
		customers.put(key, c);
		return c;
	}

	public Optional<Customer> getCustomerById(String id) {
		return Optional.ofNullable(customers.get(id));
	}
	
	public Collection<Product> getAllProducts() {
		return products.values();
	}

	public Optional<Product> getProductById(String id) {
//		System.out.println(String.format(">> getProductById(%s) %tT %s", productId, new Date(), Thread.currentThread()));
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		System.out.println(String.format("<< getProductById %tT %s", new Date(), Thread.currentThread()));

		return Optional.ofNullable(products.get(id));
	}
	
	public Rating rateProduct(String productId, String customerId, int score, String comment) {
//		System.out.println(String.format(">> rateProduct(%s) %tT %s", productId, new Date(), Thread.currentThread()));
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		System.out.println(String.format("<< rateProduct %tT %s", new Date(), Thread.currentThread()));

		Product product = getProductById(productId).get();
		Customer customer = getCustomerById(customerId).get();
		Rating rating = new Rating(UUID.randomUUID().toString(), product, customer, score, comment); 
		
		if(cRatings.get(customer) == null) cRatings.put(customer, new ArrayList<>());
		if(pRatings.get(product) == null) pRatings.put(product, new ArrayList<>());
		cRatings.get(customer).add(rating);
		pRatings.get(product).add(rating);
		return rating;
	}
	
	public List<Rating> getRatingsForCustomer(Customer c) {
		return cRatings.getOrDefault(c, new ArrayList<Rating>());
	}

	public List<Rating> getRatingsForProduct(Product p) {
		return pRatings.getOrDefault(p, new ArrayList<Rating>());
	}

}
