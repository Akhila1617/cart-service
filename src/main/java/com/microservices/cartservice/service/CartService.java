package com.microservices.cartservice.service;

import com.microservices.cartservice.dto.ProductResponse;
import com.microservices.cartservice.model.Cart;
import com.microservices.cartservice.producer.CartEventProducer;
import com.microservices.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class CartService {

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

        ProductResponse product = webClient.get()
                .uri("/product/" + cart.getProductId())
                .retrieve()
                .bodyToMono(ProductResponse.class)
                .block();

        System.out.println("Product fetched using WebClient: " + product.getName());

        Cart savedCart = cartRepository.save(cart);

        String eventMessage = "{ \"cartId\": " + savedCart.getId()
                + ", \"productId\": " + savedCart.getProductId()
                + ", \"quantity\": " + savedCart.getQuantity()
                + " }";

        cartEventProducer.sendCartEvent(eventMessage);

        return savedCart;
    }

    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }
}