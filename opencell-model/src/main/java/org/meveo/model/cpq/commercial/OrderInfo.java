package org.meveo.model.cpq.commercial;

import jakarta.persistence.*;
import org.meveo.model.cpq.ProductVersion;

@Embeddable
public class OrderInfo {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CommercialOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_version_id")
    private ProductVersion productVersion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_lot_id")
    private OrderLot orderLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_product_id")
    private OrderProduct orderProduct;

    /**
     * added for performance reason in the front
     */
    @Transient
    private OrderOffer orderOffer;

    /**
     * @return the order
     */
    public CommercialOrder getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(CommercialOrder order) {
        this.order = order;
    }

    /**
     * @return the productVersion
     */
    public ProductVersion getProductVersion() {
        return productVersion;
    }

    /**
     * @param productVersion the productVersion to set
     */
    public void setProductVersion(ProductVersion productVersion) {
        this.productVersion = productVersion;
    }

    public void setOrderLot(OrderLot orderLot) {
        this.orderLot = orderLot;
    }

    public OrderLot getOrderLot() {
        return orderLot;
    }

    public OrderProduct getOrderProduct() {
        return orderProduct;
    }

    public void setOrderProduct(OrderProduct orderProduct) {
        this.orderProduct = orderProduct;
    }

    public OrderOffer getOrderOffer() {
        return orderOffer;
    }
    public void setOrderOffer(OrderOffer orderOffer) {
        this.orderOffer = orderOffer;
    }

}