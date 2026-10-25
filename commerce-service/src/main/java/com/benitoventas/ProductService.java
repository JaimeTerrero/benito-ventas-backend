package com.benitoventas;

import java.math.BigDecimal;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped 
public class ProductService {
    public List<ProductResponse> list(){
        return List.of(
            new ProductResponse("1", "Cafe", new BigDecimal("150.00")),
            new ProductResponse("2", "Arroz", new BigDecimal("80.00"))
        );
    }

    public ProductResponse findById(String id){
        return list().stream()
                .filter(product -> product.id().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException("Producto no encontrado"));
    }
}
