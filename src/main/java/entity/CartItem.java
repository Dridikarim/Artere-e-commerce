package entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class CartItem {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private Product product;

    private int quantity;

    @ManyToOne(optional = false)
    @JsonIgnore
    private Cart cart;
}