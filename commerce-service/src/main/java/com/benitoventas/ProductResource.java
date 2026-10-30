package com.benitoventas;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.NotNull;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {
    
    private final ProductService productService;

    @Inject 
    public ProductResource(ProductService productService){
        this.productService = productService;
    }

    @GET
    @PermitAll
    public List<ProductResponse> list(){
        return productService.list();
    }

    @GET 
    @Path("/{id}")
    @PermitAll
    public ProductResponse finById(@PathParam("id") String id){
        return productService.findById(id);
    }

    @POST
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@NotNull @Valid CreateProductRequest request, @Context UriInfo UriInfo){
        ProductResponse product = productService.create(request);

        return Response.status(Response.Status.CREATED)
                .entity(product)
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProductResponse update(@PathParam("id") String id, @Valid UpdateProductRequest request) {

        return productService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response deactivate(@PathParam("id") String id) {
        productService.deactivate(id);

        return Response.noContent().build();
    }
}
