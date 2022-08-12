package com.example.stripeintegration.payment.stripe;

import com.example.stripeintegration.payment.dtos.CardDto;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.Price;
import com.stripe.model.SetupIntent;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tr.com.kron.cloudpam.crm.SubscriptionPackagePricingReference;
import tr.com.kron.cloudpam.crm.SubscriptionPackageService;
import tr.com.kron.cloudpam.payment.dtos.CardDto;
import tr.com.kron.cloudpam.tenant.TenantService;
import tr.com.kron.cloudpam.user.User;
import tr.com.kron.cloudpam.user.UserService;
import tr.com.kron.cloudpam.user.UserTenantStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentService {
    private final SubscriptionPackageService packageService;
    private final TenantService tenantService;
    private final UserService userService;

    @Value("${client-domain}")
    private String clientDomain;
    @Value("${stripe-success-url}")
    private String successUrl;
    @Value("${stripe-cancel-url}")
    private String cancelUrl;

    @SneakyThrows
    public String createPaymentForm() {
        User currentUser = userService.getCurrentUser();
        SubscriptionPackagePricingReference packagePricingReference = packageService.getSuitablePricingReference(currentUser.getCompany().getSubscriptionPackage(), currentUser.getCompany().getEndpointCount(), currentUser.getCompany().isAnnual()).get();

        if (Objects.isNull(packagePricingReference)) {
            packageService.createPricingReferenceIfNotExistsForStripe(currentUser.getCompany().getSubscriptionPackage(), currentUser.getCompany().getEndpointCount(), currentUser.getCompany().isAnnual());
        }

        Price price = Price.retrieve(packagePricingReference.getReferenceId());


        if (Objects.isNull(currentUser.getCompany().getPaymentCustomerReferenceCode())) {
            throw new tr.com.kron.cloudpam.payment.stripe.exceptions.StripeException("Customer Reference Id Not Found");
        }

        Customer customer = Customer.retrieve(currentUser.getCompany().getPaymentCustomerReferenceCode());

        if (Objects.isNull(customer)) {
            throw new tr.com.kron.cloudpam.payment.stripe.exceptions.StripeException("Stripe Customer Not Found");
        }

        SubscriptionCollection subscriptionCollection = Subscription.list(SubscriptionListParams.builder()
                .setCustomer(customer.getId())
                .build());

        if (subscriptionCollection.getData().isEmpty()) {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setCustomer(customer.getId())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(price.getId())
                                    .setQuantity(1L)
                                    .build())
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(clientDomain + successUrl)
                    .setCancelUrl(clientDomain + cancelUrl)
                    .build();
            Session session = Session.create(params);
            return session.getUrl();
        }

        Subscription subscription = Subscription.retrieve(subscriptionCollection.getData().get(0).getId());

        if (Objects.isNull(subscription)) {
            throw new tr.com.kron.cloudpam.payment.stripe.exceptions.StripeException("Stripe Subscription Not Found");
        }

        SubscriptionUpdateParams subscriptionUpdateParams = SubscriptionUpdateParams.builder()
                .addItem(
                        SubscriptionUpdateParams
                                .Item.builder()
                                .setId(subscription.getItems().getData().get(0).getId())
                                .setPrice(price.getId())
                                .build()
                )
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.NONE)
                .setCancelAtPeriodEnd(false)
                .build();


        Subscription updatedSubscription = subscription.update(subscriptionUpdateParams);

        if (Objects.isNull(updatedSubscription) && updatedSubscription.getStatus() != "active") {
            throw new tr.com.kron.cloudpam.payment.stripe.exceptions.StripeException("Subscription Not Active!");
        }

        currentUser.getCompany().setPaymentSubscriptionReferenceCode(updatedSubscription.getItems().getData().get(0).getId());
        currentUser.getCompany().setPaymentSubscriptionParentReferenceCode(updatedSubscription.getId());

        currentUser = userService.save(currentUser);

        if (currentUser.getCompany().getTenant() != null && currentUser.getCompany().getTenantStatus() != UserTenantStatus.NOT_PURCHASED) {
            tenantService.upgradeAsync(currentUser);
        } else {
            tenantService.generateAsync(currentUser);
        }

        return "Success";

    }


    @SneakyThrows
    public boolean checkResult(String sessionId) {
        User currentUser = userService.getCurrentUser();
        Session checkoutSession = null;
        try {
            checkoutSession = Session.retrieve(sessionId);

            if (checkoutSession.getStatus().equals("complete") && checkoutSession.getPaymentStatus().equals("paid")) {
                if (currentUser.getCompany().getTenant() != null && currentUser.getCompany().getTenantStatus() != UserTenantStatus.NOT_PURCHASED) {
                    tenantService.upgradeAsync(currentUser);
                } else {
                    tenantService.generateAsync(currentUser);
                }

                Subscription subscription = Subscription.list(SubscriptionListParams.builder()
                        .setCustomer(checkoutSession.getCustomer())
                        .build()).getData().get(0);

                currentUser.getCompany().setPaymentSubscriptionReferenceCode(subscription.getItems().getData().get(0).getId());
                currentUser.getCompany().setPaymentSubscriptionParentReferenceCode(subscription.getId());

                userService.save(currentUser);

                return true;
            }
            return false;

        } catch (StripeException exception) {
            throw new tr.com.kron.cloudpam.payment.stripe.exceptions.StripeException(exception.getMessage());
        }

    }


    @SneakyThrows
    public boolean cancelAndReActiveSubscriptionAtPeriodEnd(String subscriptionId, boolean canceled) {
        Subscription subscription;
        try {
            subscription = Subscription.retrieve(subscriptionId);

            SubscriptionUpdateParams params =
                    SubscriptionUpdateParams.builder()
                            .setCancelAtPeriodEnd(canceled)
                            .build();

            subscription = subscription.update(params);

        } catch (StripeException e) {
            throw new tr.com.kron.cloudpam.payment.stripe.exceptions.StripeException(e.getMessage());
        }

        return subscription.getCancelAtPeriodEnd() == canceled;
    }

    @SneakyThrows
    public Subscription getSubscription(String subscriptionId) {
        try {
            return Subscription.retrieve(subscriptionId);
        } catch (StripeException exception) {
            return null;
        }
    }

    @SneakyThrows
    public Invoice getInvoice(String invoiceId) {
        try {
            return Invoice.retrieve(invoiceId);
        } catch (StripeException exception) {
            return null;
        }
    }

    @SneakyThrows
    public String createSetupIntent(User user) {
        SetupIntent setupIntent;
        try {
            SetupIntentCreateParams params =
                    SetupIntentCreateParams
                            .builder()
                            .setCustomer(user.getCompany().getPaymentCustomerReferenceCode())
                            .addPaymentMethodType("card")
                            .build();

            setupIntent = SetupIntent.create(params);
        } catch (StripeException e) {
            throw new tr.com.kron.cloudpam.payment.stripe.exceptions.StripeException(e.getMessage());
        }
        return setupIntent.getClientSecret();
    }


    @SneakyThrows
    public PaymentMethodCollection getUserPaymentMethods(User user) {
        PaymentMethodCollection paymentMethods;

        try {

            PaymentMethodListParams params =
                    PaymentMethodListParams
                            .builder()
                            .setCustomer(user.getCompany().getPaymentCustomerReferenceCode())
                            .setType(PaymentMethodListParams.Type.CARD)
                            .build();

            paymentMethods = PaymentMethod.list(params);

        } catch (StripeException e) {
            throw new tr.com.kron.cloudpam.payment.stripe.exceptions.StripeException(e.getMessage());

        }

        return paymentMethods;
    }


    @SneakyThrows
    public List<PaymentMethod> detachPaymentMethod(PaymentMethodCollection paymentMethodCollection) {
        List<PaymentMethod> deletedPaymentMethodList = new ArrayList<>();
        paymentMethodCollection.getData().forEach(paymentMethod -> {
            try {
                deletedPaymentMethodList.add(PaymentMethod.retrieve(paymentMethod.getId()).detach());
            } catch (StripeException e) {
                e.printStackTrace();
            }

        });

        return deletedPaymentMethodList;
    }

    @SneakyThrows
    public Subscription setSubscriptionDefaultPaymentMethod(User user) {
        try {
            Subscription subscription = Subscription.retrieve(user.getCompany().getPaymentSubscriptionParentReferenceCode());

            SubscriptionUpdateParams params =
                    SubscriptionUpdateParams.builder()
                            .setDefaultPaymentMethod(getUserPaymentMethods(user).getData().get(0).getId())
                            .build();

            return subscription.update(params);

        } catch (StripeException e) {
            throw new tr.com.kron.cloudpam.payment.stripe.exceptions.StripeException(e.getMessage());
        }
    }

    @SneakyThrows
    public ResponseEntity<CardDto> getSubscriptionPaymentMethodDefaultCard(User user) {
        try {
            Subscription subscription = null;
            Customer customer = null;

            if (Objects.isNull(user.getCompany().getPaymentSubscriptionParentReferenceCode())) {
                customer = Customer.retrieve(user.getCompany().getPaymentCustomerReferenceCode());

                SubscriptionCollection subscriptionCollection = Subscription.list(SubscriptionListParams.builder()
                        .setCustomer(customer.getId())
                        .build());

                subscription = Subscription.retrieve(subscriptionCollection.getData().get(0).getId());
                user.getCompany().setPaymentSubscriptionReferenceCode(subscription.getItems().getData().get(0).getId());
                user.getCompany().setPaymentSubscriptionParentReferenceCode(subscription.getId());
                user = userService.save(user);

            } else {
                subscription = Subscription.retrieve(user.getCompany().getPaymentSubscriptionParentReferenceCode());

            }


            if (Objects.isNull(subscription.getDefaultPaymentMethod())) {
                return ResponseEntity.notFound().build();
            }

            PaymentMethod paymentMethod = PaymentMethod.retrieve(subscription.getDefaultPaymentMethod());

            if (Objects.isNull(paymentMethod.getCard())) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(CardDto.convertDto(paymentMethod.getCard()));

        } catch (StripeException e) {
            throw new tr.com.kron.cloudpam.payment.stripe.exceptions.StripeException(e.getMessage());
        }
    }
}