package com.benitoventas;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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

    @Inject
    public CustomerResource(CustomerService customerService) {
        this.customerService = customerService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateCustomerRequest request){
        var customer = customerService.create(request);

        return Response.status(Response.Status.CREATED).entity(customer).build();
    }

    @POST
    @Path("/{id}/deposits")
    @Consumes(MediaType.APPLICATION_JSON)
    public CustomerResponse deposit(@PathParam("id") String id, @Valid DepositRequest request){
        return customerService.deposit(id, request);
    }

    @GET 
    @Path("/{id}")
    public CustomerResponse findById(@PathParam("id") String id){
        return customerService.findById(id);
    }
}
