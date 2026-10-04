package service;

import dto.ProductRequest;
import entity.Product;
import exception.NotFoundException;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.CartItemRepository;
import repository.ProductRepository;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository products;
    private final CartItemRepository cartItems;

    public ProductService(ProductRepository products, CartItemRepository cartItems) {
        this.products = products;
        this.cartItems = cartItems;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return products.findAll();
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new NotFoundException("Produit introuvable: " + id));
    }

    public Product create(ProductRequest r) {
        Product p = new Product();
        apply(p, r);
        return products.save(p);
    }

    public Product update(Long id, ProductRequest r) {
        Product p = get(id);
        apply(p, r);
        return p;
    }

    public void delete(Long id) throws BadRequestException {
        Product p = get(id);
        if (cartItems.existsByProductId(id)) {
            throw new BadRequestException("Produit présent dans un panier, suppression impossible");
        }
        p.getCategories().forEach(c -> c.getProducts().remove(p)); // supprime les liens
        products.delete(p);
    }

    private void apply(Product p, ProductRequest r) {
        p.setName(r.name());
        p.setPrice(r.price());
        p.setStock(r.stock());
    }
}

