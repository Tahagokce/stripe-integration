package com.example.stripeintegration.payment.stripe.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MailContentDto {
    private String companyName;
    private String amountOwedOnInvoice;
    private String invoiceReferenceNumber;
    private String dateDue;
}
