package entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String description;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private Category parent;


    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE)
    private List<Category> children = new ArrayList<>();


    @ManyToMany
    @JoinTable(name = "category_product",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<Product> products = new HashSet<>();
}