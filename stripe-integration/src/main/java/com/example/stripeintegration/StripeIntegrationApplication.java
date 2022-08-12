package com.example.stripeintegration;

import com.stripe.Stripe;
import lombok.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class StripeIntegrationApplication {

    @Value("${stripe-secret-key}")
    private String apiKey ;

    @PostConstruct
    public void setup (){
        Stripe.apiKey = apiKey;
    }

    public static void main(String[] args) {
        SpringApplication.run(StripeIntegrationApplication.class, args);
    }

}
