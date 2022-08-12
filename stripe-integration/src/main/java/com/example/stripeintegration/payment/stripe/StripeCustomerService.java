package com.example.stripeintegration.payment.stripe;

import com.example.stripeintegration.payment.stripe.exceptions.StripeException;
import com.example.stripeintegration.payment.stripe.utils.StripeConstants;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.CustomerUpdateParams;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class StripeCustomerService {

    @SneakyThrows
    public CustomerCollection fetchCustomerList(CustomerListParams params) {
            return Customer.list(params);
    }

    @SneakyThrows
    public Customer customerCreate(CustomerCreateParams params) {
        return Customer.create(params);
    }

    @SneakyThrows
    public Customer updateCustomer(String customerId, CustomerUpdateParams params) {
        Customer customer = Customer.retrieve(customerId);
         if (Objects.isNull(customer)){
             throw new StripeException(StripeConstants.CUSTOMER_NOT_FOUND);
         }
        return customer.update(params);
    }

    @SneakyThrows
    public Customer fetchCustomer(String customerId) {
        Customer customer = Customer.retrieve(customerId);
         if (Objects.isNull(customer)){
             throw new StripeException(StripeConstants.CUSTOMER_NOT_FOUND);
         }
        return customer;
    }

    @SneakyThrows
    public Customer deleteCustomer(String customerId) {
        Customer customer = Customer.retrieve(customerId);
        if (Objects.isNull(customer)){
            throw new StripeException(StripeConstants.CUSTOMER_NOT_FOUND);
        }
        return customer.delete();
    }
}
