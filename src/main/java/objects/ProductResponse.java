package objects;

import enums.Currency;
import lombok.Data;

@Data
public class ProductResponse {
    private String title;
    private String description;
    private Double weight;
    private Double height;
    private Double length;
    private Double width;
    private Double price;
    private Currency currency;
    private Double availableStock;
    private Integer row;
    private Integer shell;
}
