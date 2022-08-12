package com.example.stripeintegration.payment.dtos;

import com.stripe.model.PaymentMethod.Card;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CardDto {
    private String brand;

    private Long expMonth;

    private Long expYear;

    private String last4;


    public static CardDto convertDto(Card card) {
        CardDto cardDto = new CardDto();
        cardDto.setBrand(card.getBrand());
        cardDto.setLast4(card.getLast4());
        cardDto.setExpMonth(card.getExpMonth());
        cardDto.setExpYear(card.getExpYear());
        return cardDto;
    }
}
