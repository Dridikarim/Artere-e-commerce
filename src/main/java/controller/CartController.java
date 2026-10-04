package controller;

import dto.CartItemRequest;
import dto.CartItemUpdateRequest;
import entity.Cart;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import service.CartService;

@RestController
@RequestMapping("/api/carts")
public class CartController {
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cart create() {
        return service.create();
    }

    /** Le JSON contient les lignes du panier et le champ "total". */
    @GetMapping("/{id}")
    public Cart get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/items")
    public Cart addItem(@PathVariable Long id, @Valid @RequestBody CartItemRequest request) throws BadRequestException {
        return service.addItem(id, request);
    }
    /** Modifie la quantité d'un produit déjà présent dans le panier. */
    @PutMapping("/{id}/items/{productId}")
    public Cart updateItem(@PathVariable Long id, @PathVariable Long productId,
                           @Valid @RequestBody CartItemUpdateRequest request) throws BadRequestException {
        return service.updateItem(id, productId, request);
    }

    @DeleteMapping("/{id}/items/{productId}")
    public Cart removeItem(@PathVariable Long id, @PathVariable Long productId) {
        return service.removeItem(id, productId);
    }
}
