package com.example.stripeintegration.payment.stripe;

import com.example.stripeintegration.payment.stripe.exceptions.StripeException;
import com.example.stripeintegration.payment.stripe.utils.StripeConstants;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentIntentCollection;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentListParams;
import com.stripe.param.PaymentIntentUpdateParams;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class StripePayment {

    @SneakyThrows
    public PaymentIntent createPaymentIntent (PaymentIntentCreateParams params){
        return PaymentIntent.create(params);
    }

    @SneakyThrows
    public PaymentIntent confirmPaymentIntent (String paymentIntentId , PaymentIntentConfirmParams params){
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        if (Objects.isNull(intent)){
            throw new StripeException(StripeConstants.PAYMENT_INTENT_NOT_FOUND);
        }
        return  intent.confirm(params);
    }

    @SneakyThrows
    public PaymentIntent getPaymentIntent (String paymentIntentId){
        return PaymentIntent.retrieve(paymentIntentId);
    }

    @SneakyThrows
    public PaymentIntent updatePaymentIntent (String paymentIntentId , PaymentIntentUpdateParams params){
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        if (Objects.isNull(intent)){
            throw new StripeException(StripeConstants.PAYMENT_INTENT_NOT_FOUND);
        }
        return  intent.update(params);
    }

    @SneakyThrows
    public PaymentIntent cancelPaymentIntent (String paymentIntentId , PaymentIntentCancelParams params){
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
        if (Objects.isNull(intent)){
            throw new StripeException(StripeConstants.PAYMENT_INTENT_NOT_FOUND);
        }
        return  intent.cancel(params);
    }

    @SneakyThrows
    public PaymentIntentCollection getAllPaymentIntents (PaymentIntentListParams params){
      return PaymentIntent.list(params);
    }
}
