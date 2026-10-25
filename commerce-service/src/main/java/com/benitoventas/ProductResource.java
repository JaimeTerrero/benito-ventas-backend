package com.benitoventas;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {
    
    private final ProductService productService;

    @Inject 
    public ProductResource(ProductService productService){
        this.productService = productService;
    }

    @GET
    public List<ProductResponse> list(){
        return productService.list();
    }

    @GET 
    @Path("/{id}")
    public ProductResponse finById(@PathParam("id") String id){
        return productService.findById(id);
    }
}
