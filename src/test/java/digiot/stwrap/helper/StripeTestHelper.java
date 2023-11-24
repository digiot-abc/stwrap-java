package digiot.stwrap.helper;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StripeTestHelper {

    private static final List<Customer> createdCustomers = new ArrayList<>();
    private static final List<Product> createdProducts = new ArrayList<>();
    private static final List<Plan> createdPlans = new ArrayList<>();
    private static final List<Coupon> createdCoupons = new ArrayList<>();
    private static final List<PaymentMethod> paymentMethods = new ArrayList<>();

    public static Customer createTestCustomer(String email) throws StripeException {
        Map<String, Object> customerParams = new HashMap<>();
        customerParams.put("email", email);
        Customer customer = Customer.create(customerParams);
        createdCustomers.add(customer);
        return customer;
    }

    public static Token createTestToken() throws StripeException {
        Map<String, Object> cardParams = new HashMap<>();
        cardParams.put("number", "4242424242424242");
        cardParams.put("exp_month", 12);
        cardParams.put("exp_year", 2030);
        cardParams.put("cvc", "123");
        Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("card", cardParams);
        Token token = Token.create(tokenParams);
        return token;
    }

    public static PaymentMethod attachTokenToCustomer(Customer customer, Token token) throws StripeException {
        CustomerUpdateParams customerUpdateParams = CustomerUpdateParams.builder()
            .setSource(token.getId())
            .build();
        customer.update(customerUpdateParams);
        String paymentMethodId = customer.getDefaultSource();
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
        paymentMethods.add(paymentMethod);
        return paymentMethod;
    }

    public static PaymentMethod attachPaymentMethodToCustomer(Customer customer, PaymentMethod paymentMethod) throws StripeException {
        PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                .setCustomer(customer.getId())
                .build();
        return paymentMethod.attach(params);
    }

    public static Product createTestProduct(String name) throws StripeException {
        Map<String, Object> productParams = new HashMap<>();
        productParams.put("name", name);
        Product product = Product.create(productParams);
        createdProducts.add(product);
        return product;
    }

    public static Plan createTestPlan(String productId, long amount, String currency, String interval) throws StripeException {
        Map<String, Object> planParams = new HashMap<>();
        planParams.put("amount", amount);
        planParams.put("currency", currency);
        planParams.put("interval", interval);
        planParams.put("product", productId);
        Plan plan = Plan.create(planParams);
        createdPlans.add(plan);
        return plan;
    }

    public static Coupon createTestCoupon(int percentOff, String duration) throws StripeException {
        Map<String, Object> couponParams = new HashMap<>();
        couponParams.put("percent_off", percentOff);
        couponParams.put("duration", duration);
        Coupon coupon = Coupon.create(couponParams);
        createdCoupons.add(coupon);
        return coupon;
    }

    public static void clean() throws StripeException {
        for (Coupon coupon : createdCoupons) {
            coupon.delete();
        }
        createdCoupons.clear();

        for (Plan plan : createdPlans) {
            plan.delete();
        }
        createdPlans.clear();

        for (Product product : createdProducts) {
            product.delete();
        }
        createdProducts.clear();

        for (PaymentMethod paymentMethod : paymentMethods) {
            paymentMethod.detach();
        }
        paymentMethods.clear();
        
        for (Customer customer : createdCustomers) {
            customer.delete();
        }
        createdCustomers.clear();
    }
}