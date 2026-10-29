package com.benitoventas;

import java.nio.charset.StandardCharsets;

import io.quarkus.elytron.security.common.BcryptUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import com.mongodb.client.model.Filters;

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
        customer.name = request.name().strip();
        customer.email = request.email().strip().toLowerCase();
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

    public List<CustomerResponse> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("La pagina debe ser >= 0 y el tamaño entre 1 y 100");
        }

        return customerRepository.find(Filters.ne("role", "ADMIN")).page(page, size)
                .list().stream().map(this::toResponse).toList();
    }

    public CustomerResponse update(String id, UpdateCustomerRequest request) {

        if (!ObjectId.isValid(id)) {
            throw new BadRequestException("El ID del cliente no tiene un formato valido");
        }

        try {
            var customer = customerRepository.updateProfile(new ObjectId(id),
                    request.name().strip(),
                    request.email().strip().toLowerCase(Locale.ROOT)
            );

            if (customer == null) {
                throw new NotFoundException("Cliente no encontrado");
            }

            return toResponse(customer);

        } catch (MongoWriteException exception) {
            if (exception.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new WebApplicationException("El correo ya esta registrado", 409);
            }

            throw exception;
        }
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
