package ServicesTests;

import dto.CategoryRequest;
import entity.Category;
import entity.Product;
import exception.BadRequestException;
import exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.CategoryRepository;
import service.CategoryService;
import service.ProductService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository categories;
    @Mock
    ProductService productService;
    @InjectMocks
    CategoryService service;

    private Category category(long id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private Product product(long id) {
        Product p = new Product();
        p.setId(id);
        return p;
    }

    // ---------- lecture ----------

    @Test
    void findRoots_returnsRepositoryResult() {
        List<Category> roots = List.of(category(1, "A"), category(2, "B"));
        when(categories.findByParentIsNull()).thenReturn(roots);

        assertThat(service.findRoots()).isEqualTo(roots);
    }

    @Test
    void get_unknownId_throwsNotFound() {
        when(categories.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(NotFoundException.class);
    }

    // ---------- création ----------

    @Test
    void create_root_hasNoParent() {
        when(categories.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        Category result = service.create(new CategoryRequest("Info", "desc", null));

        assertThat(result.getName()).isEqualTo("Info");
        assertThat(result.getDescription()).isEqualTo("desc");
        assertThat(result.getParent()).isNull();
    }

    @Test
    void create_withParent_linksBothSides() {
        Category parent = category(1, "Parent");
        when(categories.findById(1L)).thenReturn(Optional.of(parent));
        when(categories.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        Category result = service.create(new CategoryRequest("Enfant", null, 1L));

        assertThat(result.getParent()).isSameAs(parent);
        assertThat(parent.getChildren()).containsExactly(result);
    }

    @Test
    void create_withUnknownParent_throwsAndDoesNotSave() {
        when(categories.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CategoryRequest("X", null, 99L)))
                .isInstanceOf(NotFoundException.class);
        verify(categories, never()).save(any());
    }

    // ---------- modification / suppression ----------

    @Test
    void update_changesNameAndDescription() {
        Category c = category(1, "Ancien");
        when(categories.findById(1L)).thenReturn(Optional.of(c));

        Category result = service.update(1L, new CategoryRequest("Nouveau", "nouvelle desc", null));

        assertThat(result.getName()).isEqualTo("Nouveau");
        assertThat(result.getDescription()).isEqualTo("nouvelle desc");
    }

    @Test
    void delete_detachesFromParentAndProducts() {
        Category parent = category(1, "Parent");
        Category child = category(2, "Enfant");
        Product p = product(10);
        child.setParent(parent);
        parent.getChildren().add(child);
        child.getProducts().add(p);
        p.getCategories().add(child);
        when(categories.findById(2L)).thenReturn(Optional.of(child));

        service.delete(2L);

        assertThat(parent.getChildren()).isEmpty();
        assertThat(p.getCategories()).isEmpty();
        verify(categories).delete(child);
    }

    // ---------- liens produit ----------

    @Test
    void linkProduct_linksBothSides() {
        Category c = category(1, "C");
        Product p = product(10);
        when(categories.findById(1L)).thenReturn(Optional.of(c));
        when(productService.get(10L)).thenReturn(p);

        service.linkProduct(1L, 10L);

        assertThat(c.getProducts()).containsExactly(p);
        assertThat(p.getCategories()).containsExactly(c);
    }

    @Test
    void unlinkProduct_removesBothSides() {
        Category c = category(1, "C");
        Product p = product(10);
        c.getProducts().add(p);
        p.getCategories().add(c);
        when(categories.findById(1L)).thenReturn(Optional.of(c));
        when(productService.get(10L)).thenReturn(p);

        service.unlinkProduct(1L, 10L);

        assertThat(c.getProducts()).isEmpty();
        assertThat(p.getCategories()).isEmpty();
    }

    @Test
    void linkProduct_unknownProduct_throwsNotFound() {
        Category c = category(1, "C");
        when(categories.findById(1L)).thenReturn(Optional.of(c));
        when(productService.get(10L)).thenThrow(new NotFoundException("Produit introuvable"));

        assertThatThrownBy(() -> service.linkProduct(1L, 10L)).isInstanceOf(NotFoundException.class);
        assertThat(c.getProducts()).isEmpty();
    }

    // ---------- liens sous-catégorie ----------

    @Test
    void linkChild_setsParentAndChildren() {
        Category parent = category(1, "Parent");
        Category child = category(2, "Enfant");
        when(categories.findById(1L)).thenReturn(Optional.of(parent));
        when(categories.findById(2L)).thenReturn(Optional.of(child));

        service.linkChild(1L, 2L);

        assertThat(child.getParent()).isSameAs(parent);
        assertThat(parent.getChildren()).containsExactly(child);
    }

    @Test
    void linkChild_movesChildFromPreviousParent() {
        Category oldParent = category(1, "Ancien");
        Category newParent = category(2, "Nouveau");
        Category child = category(3, "Enfant");
        child.setParent(oldParent);
        oldParent.getChildren().add(child);
        when(categories.findById(2L)).thenReturn(Optional.of(newParent));
        when(categories.findById(3L)).thenReturn(Optional.of(child));

        service.linkChild(2L, 3L);

        assertThat(oldParent.getChildren()).isEmpty();
        assertThat(newParent.getChildren()).containsExactly(child);
        assertThat(child.getParent()).isSameAs(newParent);
    }

    @Test
    void linkChild_ontoItself_isRejected() {
        Category c = category(1, "A");
        when(categories.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.linkChild(1L, 1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void linkChild_ontoItsOwnDescendant_isRejected() {
        Category a = category(1, "A");
        Category b = category(2, "B");
        Category c = category(3, "C");
        b.setParent(a);
        c.setParent(b);
        when(categories.findById(3L)).thenReturn(Optional.of(c));
        when(categories.findById(1L)).thenReturn(Optional.of(a));

        // tenter de mettre A sous C (C descend de A) créerait un cycle
        assertThatThrownBy(() -> service.linkChild(3L, 1L)).isInstanceOf(BadRequestException.class);
        assertThat(a.getParent()).isNull();
    }

    @Test
    void unlinkChild_makesChildARoot() {
        Category parent = category(1, "Parent");
        Category child = category(2, "Enfant");
        child.setParent(parent);
        parent.getChildren().add(child);
        when(categories.findById(1L)).thenReturn(Optional.of(parent));
        when(categories.findById(2L)).thenReturn(Optional.of(child));

        service.unlinkChild(1L, 2L);

        assertThat(child.getParent()).isNull();
        assertThat(parent.getChildren()).isEmpty();
    }

    @Test
    void unlinkChild_wrongParent_isRejected() {
        Category parent = category(1, "Parent");
        Category other = category(2, "Autre");
        when(categories.findById(1L)).thenReturn(Optional.of(parent));
        when(categories.findById(2L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.unlinkChild(1L, 2L)).isInstanceOf(BadRequestException.class);
    }
}

