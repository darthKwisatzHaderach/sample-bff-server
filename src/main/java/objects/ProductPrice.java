package objects;

import enums.Currency;
import lombok.Data;

@Data
public class ProductPrice {
    private double price;
    private Currency currency;
}
