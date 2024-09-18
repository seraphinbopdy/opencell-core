package org.meveo.model.billing;

import static jakarta.persistence.FetchType.LAZY;

import java.math.BigDecimal;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.AuditableEntity;
import org.meveo.model.cpq.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_amount")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "product_amount_seq"), @Parameter(name = "increment_size", value = "1") })
@NamedQueries({ @NamedQuery(name = "ProductAmount.deleteByBillingReport", query = "DELETE FROM ProductAmount WHERE billingRunReport.id = :billingRunReportId") })
public class ProductAmount extends AuditableEntity {

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "amount", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal amount;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "billing_run_report_id")
    private BillingRunReport billingRunReport;

    @Column(name = "rated_transaction_count", precision = NB_PRECISION, scale = NB_DECIMALS)
    private BigDecimal ratedTransactionCount;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BillingRunReport getBillingRunReport() {
        return billingRunReport;
    }

    public void setBillingRunReport(BillingRunReport billingRunReport) {
        this.billingRunReport = billingRunReport;
    }

    public BigDecimal getRatedTransactionCount() {
        return ratedTransactionCount;
    }

    public void setRatedTransactionCount(BigDecimal ratedTransactionCount) {
        this.ratedTransactionCount = ratedTransactionCount;
    }
}
