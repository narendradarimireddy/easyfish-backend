package com.easyfish.backend3.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "user_phone"}))
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Integer getStars() {
		return stars;
	}

	public void setStars(Integer stars) {
		this.stars = stars;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@ManyToOne(optional = false)
    @JsonIgnoreProperties({"description", "additionalImagesText"})
    private Product product;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_phone", referencedColumnName = "phone", nullable = false)
    @JsonIgnoreProperties({"password", "otp", "role", "verified"})
    private User user;

    private Integer stars;

    @Column(length = 1000)
    private String comment;
}
