package com.benitoventas;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {
    private final CustomerService customerService;
    private final JsonWebToken jwt;

    @Inject
    public CustomerResource(CustomerService customerService, JsonWebToken jwt) {
        this.customerService = customerService;
        this.jwt = jwt;
    }

    @POST
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateCustomerRequest request){
        var customer = customerService.create(request);

        return Response.status(Response.Status.CREATED).entity(customer).build();
    }

    @POST
    @Path("/{id}/deposits")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    public CustomerResponse deposit(@PathParam("id") String id, @Valid DepositRequest request){
        return customerService.deposit(id, request);
    }

    @GET 
    @Path("/{id}")
    @RolesAllowed({"CLIENTE", "ADMIN"})
    public CustomerResponse findById(@PathParam("id") String id){
        boolean isAdmin = jwt.getGroups().contains("ADMIN");
        boolean isOwner = id.equals(jwt.getSubject());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("No puedes consultar otro cliente");
        }

        return customerService.findById(id);
    }
}
