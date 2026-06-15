package com.microservices.cartservice.service;

import com.microservices.cartservice.dto.ProductResponse;
import com.microservices.cartservice.model.Cart;
import com.microservices.cartservice.producer.CartEventProducer;
import com.microservices.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CartService {

    private static final Logger logger =
            LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final WebClient webClient;
    private final CartEventProducer cartEventProducer;

    public CartService(CartRepository cartRepository,
                       WebClient webClient,
                       CartEventProducer cartEventProducer) {
        this.cartRepository = cartRepository;
        this.webClient = webClient;
        this.cartEventProducer = cartEventProducer;
    }

    public Cart addCart(Cart cart) {

        CompletableFuture<ProductResponse> productFuture =
                CompletableFuture.supplyAsync(() ->
                        webClient.get()
                                .uri("/product/" + cart.getProductId())
                                .retrieve()
                                .bodyToMono(ProductResponse.class)
                                .block()
                );

        CompletableFuture<Boolean> stockFuture =
                CompletableFuture.supplyAsync(() -> cart.getQuantity() > 0);

        ProductResponse product = productFuture.join();
        Boolean isStockValid = stockFuture.join();

        if (!isStockValid) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        logger.info("Product fetched asynchronously: {}", product.getName());
        logger.info("Stock validation completed successfully");


        Cart savedCart = cartRepository.save(cart);

        String eventMessage = "{ \"cartId\": " + savedCart.getId()
                + ", \"productId\": " + savedCart.getProductId()
                + ", \"quantity\": " + savedCart.getQuantity()
                + " }";

        logger.info("Cart event published to Kafka: {}", eventMessage);
        cartEventProducer.sendCartEvent(eventMessage);

        return savedCart;
    }

    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }
}