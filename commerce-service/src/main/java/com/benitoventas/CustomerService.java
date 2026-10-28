package com.benitoventas;

import java.nio.charset.StandardCharsets;

import io.quarkus.elytron.security.common.BcryptUtil;

import java.math.BigDecimal;
import java.time.Instant;

import org.bson.types.ObjectId;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped 
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }

    public CustomerResponse create(CreateCustomerRequest request){
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("La contraseña es demasiada larga");
        }

        var customer = new Customer();
        customer.name = request.name();
        customer.email = request.email().toLowerCase();
        customer.balance = new BigDecimal("0.00");
        customer.createdAt = Instant.now();
        customer.passwordHash = BcryptUtil.bcryptHash(request.password());
        customer.role = "CLIENTE";

        try {
            customerRepository.persist(customer);
        } catch (MongoWriteException exception) {
            if(exception.getError().getCategory() == ErrorCategory.DUPLICATE_KEY){
                throw new WebApplicationException("El correo ya está registrado", 409);
            }

            throw exception;
        }

        return toResponse(customer);
    }

    public CustomerResponse findById(String id){
        if(!ObjectId.isValid(id)){
            throw new BadRequestException("El ID del cliente no tiene un formato válido");
        }

        var customer = customerRepository.findById(new ObjectId(id));

        if (customer == null){
            throw new NotFoundException("No se encontro el cliente");
        }

        return toResponse(customer);
    }

    public CustomerResponse deposit(String id, DepositRequest request){
        if (!ObjectId.isValid(id)){
            throw new BadRequestException("El ID del cliente no tiene un formato válido");
        }

        var customerId = new ObjectId(id);
        var reference = request.reference().strip();

        var updatedCustomerCredit = customerRepository.creditDeposit(customerId, reference, request.amount());

        if (updatedCustomerCredit != null){
            return toResponse(updatedCustomerCredit);
        }

        var customer = customerRepository.findById(customerId);

        if (customer == null){
            throw new NotFoundException("Cliente no encontrado");
        }

        var previousCustomerDeposit = customer.deposits.stream()
                .filter(deposit -> deposit.reference.equals(reference))
                .findFirst()
                .orElseThrow(() -> 
                        new IllegalStateException("No se pudo localizar el deposito registrado"));

        if (previousCustomerDeposit.amount.compareTo(request.amount()) != 0) {
            throw new WebApplicationException("La referencia ya fue utilizada con otro monto", 409);
        }
                 
        return toResponse(customer);
    
    }

    private CustomerResponse toResponse(Customer customer){
        return new CustomerResponse(
            customer.id.toHexString(), 
            customer.name, 
            customer.email, 
            customer.balance, 
            customer.createdAt
        );
    }
}
