package com.example.stripeintegration.payment;

import com.example.stripeintegration.payment.stripe.StripePaymentService;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@AllArgsConstructor
@Log4j2
public class PaymentController {
    private final Environment env;
    private final UserService userService;
    private final PaymentService paymentService;
    private final StripePaymentService stripePaymentService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/start-check")
    public ResponseEntity<String> checkPaymentNeeded() {
        try {
            var currentUser = userService.getCurrentUser();
            if (currentUser.getCompany().getSubscriptionPackage().isFree()) {
                return ResponseEntity.ok().body("Success.");
            }
            paymentService.checkIsSuitableForPayment(currentUser);
            return ResponseEntity.ok().body("You need to do the payment for service installation.");
        } catch (PaymentNotSuitableException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }




    @PreAuthorize("isAuthenticated()")
    @PostMapping("/start")
    public ResponseEntity<String> startPayment() {
        try {
            var currentUser = userService.getCurrentUser();
            paymentService.checkIsSuitableForPayment(currentUser);
            // paymentService.createSubscriptionPaymentForm(currentUser);
            String stripePaymentFormUrl = stripePaymentService.createPaymentForm();
            return ResponseEntity.ok(stripePaymentFormUrl);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/form")
    public ModelAndView paymentForm() {
        var user = userService.getCurrentUser();
        return new ModelAndView("payment/form", Map.of("paymentForm", paymentService.getPaymentForm(user)));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/success")
    public ModelAndView paymentFormResponseSuccess() {
        return new ModelAndView("payment/success");
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/error")
    public ModelAndView paymentFormResponseError() {
        return new ModelAndView("payment/error");
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/result")
    public ResponseEntity<String> checkResult(@RequestParam String sessionId) {
        try {
            if (stripePaymentService.checkResult(sessionId)) {
                return ResponseEntity.ok("SUCCESS");
            }
            return ResponseEntity.badRequest().body("Payment failed, please contact our team.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @SneakyThrows
    @RequestMapping(value = "/process", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void processPayment(@RequestParam("requestId") UUID requestId, HttpServletResponse response) {
        String baseUrl = env.getProperty("cp.payment.callback.base-url", "http://localhost:3000");

        try {
            paymentService.processByRequestId(requestId);
            response.sendRedirect(baseUrl.concat("/api/v1/payment/success"));
        } catch (Exception e) {
            log.error(e);
            response.sendRedirect(baseUrl.concat("/api/v1/payment/error?msg=") + URLEncoder.encode(e.getMessage(),
                    StandardCharsets.UTF_8));
        }
    }

    @PostMapping("/upgrade")
    public ResponseEntity<String> upgradeProcess(@RequestBody UserPackageUpdateDTO packageUpdateDTO) {
        try {
            paymentService.upgradePackage(userService.getCurrentUser(), packageUpdateDTO);
            return ResponseEntity.ok("SUCCESS");
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/client-secret")
    public ResponseEntity<String> clientSecret() {
        return ResponseEntity.ok(stripePaymentService.createSetupIntent(userService.getCurrentUser()));
    }

    @PostMapping("/detach-payment-method")
    public ResponseEntity<String> detachPaymentMethod() {
        List<PaymentMethod> paymentMethods = stripePaymentService.detachPaymentMethod(stripePaymentService.getUserPaymentMethods(userService.getCurrentUser()));
        return ResponseEntity.ok("Success");
    }

    @GetMapping("/subscription-payment-method-card")
    public ResponseEntity<CardDto> subscriptionGetPaymentMethod() {
        return stripePaymentService.getSubscriptionPaymentMethodDefaultCard(userService.getCurrentUser());
    }

    @PostMapping("/set-default-payment-method")
    public ResponseEntity<String> setSubscriptionDefaultPaymentMethod() {
        Subscription subscription = stripePaymentService.setSubscriptionDefaultPaymentMethod(userService.getCurrentUser());
        return ResponseEntity.ok("Success");
    }

}
