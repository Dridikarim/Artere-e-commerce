package service;

import dto.CategoryRequest;
import entity.Category;
import entity.Product;
import exception.BadRequestException;
import exception.NotFoundException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.CategoryRepository;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categories;
    private final ProductService productService;

    public CategoryService(CategoryRepository categories, ProductService productService) {
        this.categories = categories;
        this.productService = productService;
    }

    // ---------- CRUD ----------

    @Transactional(readOnly = true)
    public List<Category> findRoots() {
        return categories.findByParentIsNull();
    }

    @Transactional(readOnly = true)
    public Category get(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new NotFoundException("Catégorie introuvable: " + id));
    }

    public Category create(CategoryRequest r) {
        Category c = new Category();
        c.setName(r.name());
        c.setDescription(r.description());
        if (r.parentId() != null) {
            Category parent = get(r.parentId());
            c.setParent(parent);
            parent.getChildren().add(c);
        }
        return categories.save(c);
    }

    public Category update(Long id, CategoryRequest r) {
        Category c = get(id);
        c.setName(r.name());
        c.setDescription(r.description());
        return c;
    }

    public void delete(Long id) {
        Category c = get(id);
        if (c.getParent() != null) {
            c.getParent().getChildren().remove(c);
        }
        c.getProducts().forEach(p -> p.getCategories().remove(c));
        categories.delete(c); // sous-catégories supprimées en cascade, produits conservés
    }

    // ---------- Liens catégorie <-> produit ----------

    public Category linkProduct(Long categoryId, Long productId) {
        Category c = get(categoryId);
        Product p = productService.get(productId);
        c.getProducts().add(p);
        p.getCategories().add(c);
        return c;
    }

    public Category unlinkProduct(Long categoryId, Long productId) {
        Category c = get(categoryId);
        Product p = productService.get(productId);
        c.getProducts().remove(p);
        p.getCategories().remove(c);
        return c;
    }

    // ---------- Liens catégorie <-> sous-catégorie ----------

    public Category linkChild(Long parentId, Long childId) {
        Category parent = get(parentId);
        Category child = get(childId);
        // interdit : une catégorie ne peut pas devenir son propre ancêtre
        for (Category a = parent; a != null; a = a.getParent()) {
            if (a == child) {
                throw new BadRequestException("Lien impossible : cela créerait un cycle");
            }
        }
        if (child.getParent() != null) {
            child.getParent().getChildren().remove(child);
        }
        child.setParent(parent);
        parent.getChildren().add(child);
        return parent;
    }

    public Category unlinkChild(Long parentId, Long childId) {
        Category parent = get(parentId);
        Category child = get(childId);
        if (child.getParent() != parent) {
            throw new BadRequestException("Cette catégorie n'est pas une sous-catégorie du parent indiqué");
        }
        parent.getChildren().remove(child);
        child.setParent(null); // redevient racine
        return parent;
    }
}

