package ServicesTests;

import dto.CartItemRequest;
import entity.Cart;
import entity.Product;
import exception.BadRequestException;
import exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.CartRepository;
import service.CartService;
import service.ProductService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    CartRepository carts;
    @Mock
    ProductService productService;
    @InjectMocks
    CartService service;

    private Product product(long id, String price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName("P" + id);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        return p;
    }

    private Cart cart(long id) {
        Cart c = new Cart();
        c.setId(id);
        return c;
    }

    @Test
    void create_savesAnEmptyCart() {
        when(carts.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = service.create();

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotal()).isEqualByComparingTo("0");
        verify(carts).save(any(Cart.class));
    }

    @Test
    void get_unknownCart_throwsNotFound() {
        when(carts.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void addItem_newProduct_createsLine() throws org.apache.coyote.BadRequestException {
        Cart cart = cart(1);
        Product p = product(10, "5.00", 10);
        when(carts.findById(1L)).thenReturn(Optional.of(cart));
        when(productService.get(10L)).thenReturn(p);

        Cart result = service.addItem(1L, new CartItemRequest(10L, 2));

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProduct()).isSameAs(p);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getItems().get(0).getCart()).isSameAs(cart);
    }

    @Test
    void addItem_sameProductTwice_mergesQuantities() throws org.apache.coyote.BadRequestException {
        Cart cart = cart(1);
        Product p = product(10, "5.00", 10);
        when(carts.findById(1L)).thenReturn(Optional.of(cart));
        when(productService.get(10L)).thenReturn(p);

        service.addItem(1L, new CartItemRequest(10L, 2));
        Cart result = service.addItem(1L, new CartItemRequest(10L, 3));

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addItem_quantityAboveStock_isRejected() {
        Cart cart = cart(1);
        Product p = product(10, "5.00", 1);
        when(carts.findById(1L)).thenReturn(Optional.of(cart));
        when(productService.get(10L)).thenReturn(p);

        assertThatThrownBy(() -> service.addItem(1L, new CartItemRequest(10L, 2)))
                .isInstanceOf(BadRequestException.class);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void addItem_mergedQuantityAboveStock_isRejectedAndKeepsPreviousQuantity() throws org.apache.coyote.BadRequestException {
        Cart cart = cart(1);
        Product p = product(10, "5.00", 5);
        when(carts.findById(1L)).thenReturn(Optional.of(cart));
        when(productService.get(10L)).thenReturn(p);
        service.addItem(1L, new CartItemRequest(10L, 3));

        assertThatThrownBy(() -> service.addItem(1L, new CartItemRequest(10L, 3)))
                .isInstanceOf(BadRequestException.class);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void addItem_unknownCart_throwsNotFound() {
        when(carts.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItem(1L, new CartItemRequest(10L, 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addItem_unknownProduct_throwsNotFound() {
        when(carts.findById(1L)).thenReturn(Optional.of(cart(1)));
        when(productService.get(10L)).thenThrow(new NotFoundException("Produit introuvable"));

        assertThatThrownBy(() -> service.addItem(1L, new CartItemRequest(10L, 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeItem_removesOnlyThatProduct() throws org.apache.coyote.BadRequestException {
        Cart cart = cart(1);
        Product p1 = product(10, "5.00", 10);
        Product p2 = product(20, "3.00", 10);
        when(carts.findById(1L)).thenReturn(Optional.of(cart));
        when(productService.get(10L)).thenReturn(p1);
        when(productService.get(20L)).thenReturn(p2);
        service.addItem(1L, new CartItemRequest(10L, 1));
        service.addItem(1L, new CartItemRequest(20L, 1));

        Cart result = service.removeItem(1L, 10L);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProduct()).isSameAs(p2);
    }
}

