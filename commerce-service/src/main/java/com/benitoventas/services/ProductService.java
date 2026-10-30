package com.benitoventas.services;

import com.benitoventas.dto.CreateProductRequest;
import com.benitoventas.dto.ProductResponse;
import com.benitoventas.dto.UpdateProductRequest;
import com.benitoventas.entities.Product;
import com.benitoventas.repositories.ProductRepository;

import java.util.List;
import org.bson.types.ObjectId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import com.mongodb.client.model.Filters;

@ApplicationScoped 
public class ProductService {

    private final ProductRepository productRepository;

    @Inject
    public ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }

    public List<ProductResponse> list() {
        return productRepository.list(Filters.ne("active", false)).stream().map(this::toResponse).toList();
    }

    public ProductResponse findById(String id){
        if (!ObjectId.isValid(id)){
            throw new BadRequestException("El ID del producto no tiene un formato válido");
        }

        var product = productRepository.findById(new ObjectId(id));

        if (product == null || !product.active){
            throw new NotFoundException("Producto no encontrado");
        }

        return toResponse(product);
    }

    public ProductResponse create(CreateProductRequest request){
        var product = new Product();
        product.name = request.name().strip();
        product.price = request.price();
        product.stock = request.stock();

        productRepository.persist(product);

        return toResponse(product);
    }

    public ProductResponse update(String id, UpdateProductRequest request) {

        if (!ObjectId.isValid(id)) {
            throw new BadRequestException("El ID del producto no tiene un formato valido");
        }

        var product = productRepository.updateDetails(
                new ObjectId(id),
                request.name().strip(),
                request.price(),
                request.active()
        );

        if (product == null) {
            throw new NotFoundException("Producto no encontrado");
        }

        return toResponse(product);
    }

    public void deactivate(String id) {
        if (!ObjectId.isValid(id)) {
            throw new BadRequestException("El ID del producto no tiene un formato valido");
        }

        if (!productRepository.deactivate(new ObjectId(id))) {
            throw new NotFoundException("Producto no encontrado");
        }
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id.toHexString(),
                product.name,
                product.price
        );
    }
}
