package service;

import dto.CartItemRequest;
import entity.Cart;
import entity.CartItem;
import entity.Product;
import exception.NotFoundException;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.CartRepository;

@Service
    @Transactional
    public class CartService {

        private final CartRepository carts;
        private final ProductService productService;

        public CartService(CartRepository carts, ProductService productService) {
            this.carts = carts;
            this.productService = productService;
        }

        public Cart create() {
            return carts.save(new Cart());
        }

        @Transactional(readOnly = true)
        public Cart get(Long id) {
            return carts.findById(id)
                    .orElseThrow(() -> new NotFoundException("Panier introuvable: " + id));
        }

        public Cart addItem(Long cartId, CartItemRequest r) throws BadRequestException {
            Cart cart = get(cartId);
            Product product = productService.get(r.productId());

            CartItem item = cart.getItems().stream()
                    .filter(i -> i.getProduct().getId().equals(product.getId()))
                    .findFirst()
                    .orElse(null);

            int newQuantity = (item == null ? 0 : item.getQuantity()) + r.quantity();
            if (newQuantity > product.getStock()) {
                throw new BadRequestException("Stock insuffisant (disponible : " + product.getStock() + ")");
            }

            if (item == null) {
                item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                cart.getItems().add(item);
            }
            item.setQuantity(newQuantity);
            return cart;
        }

        public Cart removeItem(Long cartId, Long productId) {
            Cart cart = get(cartId);
            cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
            return cart;
        }
    }

