package com.example.stripeintegration.payment.stripe.dtos;

import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private String customerEmail ;
    private String customerName ;
    private String customerPhone ;
    private Long total ;
    private Long subTotal ;
    private String periodStart ;
    private String periodEnd ;
    private String status ;
    private boolean paid ;
    private String number ;
    private String invoicePdf ;
    private String hostedInvoiceUrl ;
    private String currency ;
    private InvoiceLineItem invoiceLineItem;
    private Long amountPaid;
    private Long amountDue;
    private Long amountRemaining;

    public static InvoiceDto convertDto (Invoice invoice){
        InvoiceDto dto = new InvoiceDto();
        dto.customerEmail = invoice.getCustomerEmail();
        dto.customerName = invoice.getCustomerName();
        dto.customerPhone = invoice.getCustomerPhone();
        dto.total = invoice.getTotal();
        dto.subTotal = invoice.getTotal();
        dto.status = invoice.getStatus();
        dto.paid = invoice.getPaid();
        dto.number = invoice.getNumber();
        dto.invoicePdf = invoice.getInvoicePdf();
        dto.hostedInvoiceUrl = invoice.getHostedInvoiceUrl();
        dto.invoiceLineItem = invoice.getLines().getData().get(0);
        dto.currency = invoice.getCurrency();
        dto.amountDue = invoice.getAmountDue();
        dto.amountPaid = invoice.getAmountPaid();
        dto.amountRemaining = invoice.getAmountRemaining();

        Timestamp startTs  = new Timestamp(invoice.getPeriodStart());
        Timestamp endTs  = new Timestamp(invoice.getPeriodEnd());

        String startDate = new Date(startTs.getTime()).toString();
        String endDate = new Date(endTs.getTime()).toString();

        dto.periodStart = startDate ;
        dto.periodEnd = endDate;
        return dto;
    }
}
