package entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private BigDecimal price;
    private int stock;

    @ManyToMany(mappedBy = "products")
    @JsonIgnore
    private Set<Category> categories = new HashSet<>();
}
