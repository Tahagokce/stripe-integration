package com.example.stripeintegration.payment.stripe;

import com.example.stripeintegration.payment.stripe.exceptions.StripeException;
import com.example.stripeintegration.payment.stripe.utils.StripeConstants;
import com.stripe.model.Product;
import com.stripe.model.ProductCollection;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductListParams;
import com.stripe.param.ProductUpdateParams;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class StripeProductService {

    @SneakyThrows
    public Product createProduct(ProductCreateParams params) {
        return Product.create(params);
    }

    @SneakyThrows
    public Product getProduct(String productId) {
        return Product.retrieve(productId);
    }

    @SneakyThrows
    public Product updateProduct(String productId, ProductUpdateParams params) {
        Product product = Product.retrieve(productId);
        if (Objects.isNull(product)) {
            throw new StripeException(StripeConstants.PRODUCT_NOT_FOUND);
        }
        return product.update(params);
    }

    @SneakyThrows
    public ProductCollection getAllProduct(ProductListParams params) {
        return Product.list(params);
    }

    @SneakyThrows
    public Product deleteProduct(String productId) {
        Product product = Product.retrieve(productId);
        if (Objects.isNull(product)) {
            throw new StripeException(StripeConstants.PRODUCT_NOT_FOUND);
        }
        return product.delete();
    }

}
