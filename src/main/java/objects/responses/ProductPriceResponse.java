package objects.responses;

import enums.Currency;
import lombok.Data;

@Data
public class ProductPriceResponse {
    private double price;
    private Currency currency;
}
