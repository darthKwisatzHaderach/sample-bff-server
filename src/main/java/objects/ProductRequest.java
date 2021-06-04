package objects;

import enums.Source;
import lombok.Data;

@Data
public class ProductRequest {
    private String productId;
    private Source source;
}
