package com.benitoventas;

import java.util.List;
import org.bson.types.ObjectId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped 
public class ProductService {

    private final ProductRepository productRepository;

    @Inject
    public ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }

    public List<ProductResponse> list() {
        return productRepository.listAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse findById(String id){
        if (!ObjectId.isValid(id)){
            throw new BadRequestException("El ID del producto no tiene un formato válido");
        }

        var product = productRepository.findById(new ObjectId(id));

        if (product == null){
            throw new NotFoundException("Producto no encontrado");
        }

        return toResponse(product);
    }

    public ProductResponse create(CreateProductRequest request){
        Product product = new Product();
        product.name = request.name().strip();
        product.price = request.price();
        product.stock = request.stock();

        productRepository.persist(product);

        return toResponse(product);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id.toHexString(),
                product.name,
                product.price
        );
    }
}
