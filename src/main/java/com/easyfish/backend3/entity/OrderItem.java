package com.easyfish.backend3.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    
    private Order order;

    @ManyToOne
    private Product product;

    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Order getOrder() {
		return order;
	}
	public void setOrder(Order order) {
		this.order = order;
	}
	public Product getProduct() {
		return product;
	}
	public void setProduct(Product product) {
		this.product = product;
	}
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	public Double getPrice() {
		return price;
	}
	public void setPrice(Double price) {
		this.price = price;
	}

    public String getPackQuantity() {
        return packQuantity;
    }
    public void setPackQuantity(String packQuantity) {
        this.packQuantity = packQuantity;
    }
    public String getPackUnit() {
        return packUnit;
    }
    public void setPackUnit(String packUnit) {
        this.packUnit = packUnit;
    }
    public Integer getTotalGrams() {
        return totalGrams;
    }
    public void setTotalGrams(Integer totalGrams) {
        this.totalGrams = totalGrams;
    }

	private int quantity;
    private Double price;
    private String packQuantity;
    private String packUnit;
    private Integer totalGrams;
}
