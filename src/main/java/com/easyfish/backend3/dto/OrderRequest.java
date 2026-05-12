package com.easyfish.backend3.dto;

import com.easyfish.backend3.entity.Order;
import com.easyfish.backend3.entity.OrderItem;
import java.util.List;

public class OrderRequest {
    private Order order;
    private List<OrderItem> items;

    public OrderRequest() {}

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
