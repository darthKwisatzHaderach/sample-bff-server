package objects;

import lombok.Data;

@Data
public class ProductStockInfo {
    private Double availableStock;
    private Integer row;
    private Integer shell;
}
