package objects;

import lombok.Data;

@Data
public class ProductInfo {
    private String title;
    private String description;
    private double weight;
    private double height;
    private double length;
    private double width;
}
