package ServicesTests;

import dto.ProductRequest;
import entity.Category;
import entity.Product;
import exception.BadRequestException;
import exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.CartItemRepository;
import repository.ProductRepository;
import service.ProductService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository products;
    @Mock
    CartItemRepository cartItems;
    @InjectMocks
    ProductService service;

    private Product product(long id, String name, String price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        return p;
    }

    @Test
    void get_unknownId_throwsNotFound() {
        when(products.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_savesProductWithRequestedFields() {
        when(products.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        Product result = service.create(new ProductRequest("Stylo", new BigDecimal("2.50"), 10));

        assertThat(result.getName()).isEqualTo("Stylo");
        assertThat(result.getPrice()).isEqualByComparingTo("2.50");
        assertThat(result.getStock()).isEqualTo(10);
    }

    @Test
    void update_changesAllFields() {
        Product p = product(1, "Ancien", "1.00", 1);
        when(products.findById(1L)).thenReturn(Optional.of(p));

        Product result = service.update(1L, new ProductRequest("Nouveau", new BigDecimal("9.99"), 7));

        assertThat(result.getName()).isEqualTo("Nouveau");
        assertThat(result.getPrice()).isEqualByComparingTo("9.99");
        assertThat(result.getStock()).isEqualTo(7);
    }

    @Test
    void delete_removesLinksWithCategoriesAndDeletes() throws org.apache.coyote.BadRequestException {
        Product p = product(1, "P", "1", 1);
        Category c = new Category();
        c.getProducts().add(p);
        p.getCategories().add(c);
        when(products.findById(1L)).thenReturn(Optional.of(p));
        when(cartItems.existsByProductId(1L)).thenReturn(false);

        service.delete(1L);

        assertThat(c.getProducts()).isEmpty();
        verify(products).delete(p);
    }

    @Test
    void delete_productInACart_isRejected() {
        Product p = product(1, "P", "1", 1);
        when(products.findById(1L)).thenReturn(Optional.of(p));
        when(cartItems.existsByProductId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BadRequestException.class);
        verify(products, never()).delete(any());
    }
}
