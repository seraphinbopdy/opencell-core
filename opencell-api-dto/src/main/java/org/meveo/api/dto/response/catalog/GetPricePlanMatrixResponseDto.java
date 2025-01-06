package org.meveo.api.dto.response.catalog;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.meveo.api.dto.response.BaseResponse;
import org.meveo.model.cpq.enums.VersionStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "Price plan matrix response")
public class GetPricePlanMatrixResponseDto extends BaseResponse {
    private static final long serialVersionUID = 1L;
    @Schema(description = "The charge code")
    private String chargeCode;
    @Schema(description = "Price plan data")
    private PricePlanData pricePlan;
    public String getChargeCode() {
        return chargeCode;
    }
    public void setChargeCode(String chargeCode) {
        this.chargeCode = chargeCode;
    }
    public PricePlanData getPricePlan() {
        return pricePlan;
    }
    public void setPricePlan(PricePlanData pricePlan) {
        this.pricePlan = pricePlan;
    }
    public static class PricePlanData {
        
        @Schema(description = "The price plan code")
        private String code;
        
        @Schema(description = "The version number") 
        private int version;
        @Schema(description = "The price value")
        private BigDecimal price;
        @Schema(description = "The price EL expression")
        private String priceEl;
        @Schema(description = "Start date of validity")
        private Date startDate;
        @Schema(description = "End date of validity")
        private Date endDate;
        @Schema(description = "Version status")
        private VersionStatusEnum status;
        
        @Schema(description = "List of column codes")
        private List<String> columns = new ArrayList<>();
        
        @Schema(description = "Matrix data mapping line IDs to column values")
        private Map<String, Map<String, String>> matrix;
        // Getters and setters
        public String getCode() {
            return code;
        }
        public void setCode(String code) {
            this.code = code;
        }
        public int getVersion() {
            return version;
        }
        public void setVersion(int version) {
            this.version = version;
        }
        public BigDecimal getPrice() {
            return price;
        }
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        public String getPriceEl() {
            return priceEl;
        }
        public void setPriceEl(String priceEl) {
            this.priceEl = priceEl;
        }
        public Date getStartDate() {
            return startDate;
        }
        public void setStartDate(Date startDate) {
            this.startDate = startDate;
        }
        public Date getEndDate() {
            return endDate;
        }
        public void setEndDate(Date endDate) {
            this.endDate = endDate;
        }
        public VersionStatusEnum getStatus() {
            return status;
        }
        public void setStatus(VersionStatusEnum status) {
            this.status = status;
        }
        public List<String> getColumns() {
            return columns;
        }
        public void setColumns(List<String> columns) {
            this.columns = columns;
        }
        public Map<String, Map<String, String>> getMatrix() {
            return matrix;
        }
        public void setMatrix(Map<String, Map<String, String>> matrix) {
            this.matrix = matrix;
        }
    }
} 