package objects.responses;

import lombok.Data;

@Data
public class ProductStockInfoResponse {
    private Double availableStock;
    private Integer row;
    private Integer shell;
}
