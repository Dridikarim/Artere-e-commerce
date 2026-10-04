package controller;

import dto.CategoryRequest;
import entity.Category;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Category> roots() {
        return service.findRoots();
    }

    @GetMapping("/{id}")
    public Category get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@Valid @RequestBody CategoryRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }


    @PostMapping("/{categoryId}/products/{productId}")
    public Category linkProduct(@PathVariable Long categoryId, @PathVariable Long productId) {
        return service.linkProduct(categoryId, productId);
    }

    @DeleteMapping("/{categoryId}/products/{productId}")
    public Category unlinkProduct(@PathVariable Long categoryId, @PathVariable Long productId) {
        return service.unlinkProduct(categoryId, productId);
    }


    @PostMapping("/{parentId}/children/{childId}")
    public Category linkChild(@PathVariable Long parentId, @PathVariable Long childId) {
        return service.linkChild(parentId, childId);
    }

    @DeleteMapping("/{parentId}/children/{childId}")
    public Category unlinkChild(@PathVariable Long parentId, @PathVariable Long childId) {
        return service.unlinkChild(parentId, childId);
    }
}
