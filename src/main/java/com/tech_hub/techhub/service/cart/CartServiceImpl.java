package com.tech_hub.techhub.service.cart;

import com.tech_hub.techhub.entity.*;
import com.tech_hub.techhub.repository.CartItemRepository;
import com.tech_hub.techhub.repository.CartRepository;
import com.tech_hub.techhub.repository.UserRepository;
import com.tech_hub.techhub.service.user.UserService;
import com.tech_hub.techhub.service.variant.VariantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartRepository cartRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    VariantService variantService;
    @Autowired
    CartItemRepository cartItemRepository;
    @Autowired
    UserService userService;

    public String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @Override
    public Cart createCart(UserEntity user) {
        Cart cart = new Cart();
        cart.setUser(user);
        user.setCart(cart);
        cartRepository.save(cart);
        return cart;
    }

    @Override
    public void deleteUserCart(UserEntity user) {
        Cart userCart = user.getCart();
        if (userCart != null){
            cartRepository.delete(userCart);
        }
    }

    @Override
    public Optional<Cart> getCart(UserEntity user) {
        return cartRepository.findByUser(user).or
                (() -> {
                    Cart newCart = createCart(user);
                    return Optional.of(newCart);
                });
    }

    @Override
    public Cart getCartByUserName(String username) {
        return cartRepository.findByUserUsername(username);
    }

    @Override
    public void addToCart(Long id, String username) {

        Variant variant = variantService.getVariantById(id).orElseThrow();
        UserEntity user = userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("user not found"));


        Cart cart = cartRepository.findByUser(user).orElseGet(() -> createCart(user));
        Optional<CartItems> optionalCartItem = cart.getCartItems().stream()
                .filter(cartItems -> cartItems.getVariant().equals(variant)).findFirst();

        if (optionalCartItem.isPresent()) {
            CartItems existingCartItem = optionalCartItem.get();
            existingCartItem.setQuantity(existingCartItem.getQuantity() + 1);
            cartItemRepository.save(existingCartItem);

        } else {
            CartItems cartItem = new CartItems();
            cartItem.setQuantity(1);
            cartItem.setVariant(variant);
            cartItem.setCart(cart);
            cart.getCartItems().add(cartItem);
            cartRepository.save(cart);
        }
    }


    @Override
    public List<CartItems> getAllItems() {
        return cartItemRepository.findAll();
    }

    @Override
    public List<CartItems> getCartItemsForUsers(String username) {

        UserEntity user = userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("user not found"));
        Optional<Cart> cart = getCart(user);

        return cart.get().getCartItems();
    }

    @Override
    public void deleteCartItemById(Long id) {
        cartItemRepository.deleteById(id);
    }

    @Override
    public void deleteCartById(Long id) {
        cartRepository.deleteById(id);
    }

    @Override
    public void deleteCart(Cart cart) {
        cartRepository.delete(cart);
    }

    @Override
    public void deleteCartItem(CartItems cartItems) {
        cartItemRepository.delete(cartItems);
    }




    @Override
    public double calculateTotalPrice(List<CartItems> cartItems) {
        return cartItems.stream().mapToDouble(cartItem ->
                cartItem.getVariant().getPrice() * cartItem.getQuantity()).sum();
    }
    @Override
    public double calculateTotalPriceWithDiscount(Double discountedTotalPrice, double totalPriceWithoutDiscount) {
        return discountedTotalPrice != null ? discountedTotalPrice : totalPriceWithoutDiscount;
    }

    @Override
    public Optional<CartItems> getCartItemById(Long id) {
        return cartItemRepository.findById(id);
    }


    @Override
    public void updateCartItem(String username, Long id, int newQuantity) {
        Optional<UserEntity> optionalUser = userService.findByUsername(username);
        try {
            if (optionalUser.isPresent()) {
                UserEntity user = optionalUser.get();
                Cart cart = user.getCart();

                List<CartItems> cartItems = cart.getCartItems();
                for (CartItems cartItem : cartItems) {
                    if (cartItem.getVariant().getId().equals(id)) {
                        cartItem.setQuantity(newQuantity);
                        cartItemRepository.save(cartItem);
                        return;
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void save(Cart cart) {
        cartRepository.save(cart);
    }

}