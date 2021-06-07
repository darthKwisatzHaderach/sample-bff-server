package objects.responses;

import lombok.Data;

@Data
public class ProductInfoResponse {
    private String title;
    private String description;
    private double weight;
    private double height;
    private double length;
    private double width;
}
