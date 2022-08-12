package com.example.stripeintegration.payment.stripe.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CreatePayment {
    Object[] items;

    public Object[] getItems() {
        return items;
    }
}
