package com.microservices.cartservice.service;

import com.microservices.cartservice.dto.ProductResponse;
import com.microservices.cartservice.entity.Cart;
import com.microservices.cartservice.entity.CartItem;
import com.microservices.cartservice.producer.CartEventProducer;
import com.microservices.cartservice.repository.CartItemRepository;
import com.microservices.cartservice.repository.CartRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartEventProducer cartEventProducer;
    private final WebClient webClient;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            CartEventProducer cartEventProducer,
            @Value("${product.service.url}") String productServiceUrl
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartEventProducer = cartEventProducer;
        this.webClient = WebClient.builder()
                .baseUrl(productServiceUrl)
                .build();
    }

    public Cart addCart(Cart cart) {
        return cartRepository.save(cart);
    }

    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    public CartItem addCartItem(CartItem cartItem) {

        ProductResponse product = webClient.get()
                .uri("/product/" + cartItem.getProductId())
                .retrieve()
                .bodyToMono(ProductResponse.class)
                .block();

        if (product == null || product.getId() == null) {
            throw new RuntimeException("Product not found");
        }

        if (product.getStock() == null || product.getStock() < cartItem.getQuantity()) {
            throw new RuntimeException("Stock is not sufficient");
        }

        CartItem savedCartItem = cartItemRepository.save(cartItem);

        String eventMessage = "{"
                + "\"cartId\": " + savedCartItem.getCartId()
                + ", \"productId\": " + savedCartItem.getProductId()
                + ", \"quantity\": " + savedCartItem.getQuantity()
                + "}";

        cartEventProducer.sendCartEvent(eventMessage);

        return savedCartItem;
    }

    public List<CartItem> getAllCartItems() {
        return cartItemRepository.findAll();
    }
}