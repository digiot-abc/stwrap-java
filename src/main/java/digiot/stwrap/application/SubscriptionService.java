package digiot.stwrap.application;

import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionUpdateParams;
import digiot.stwrap.domain.model.StripeSubscription;
import digiot.stwrap.domain.model.StripeSubscriptionFactory;
import digiot.stwrap.domain.repository.StripeSubscriptionRepository;
import digiot.stwrap.domain.subscription.SubscriptionItemFactory;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class SubscriptionService<T> {

    private final CustomerService<T> customerService;
    private final StripeSubscriptionRepository subscriptionRepository;

    /**
     * Creates a new subscription for a user in Stripe using an existing payment method ID.
     *
     * @param userId           The user ID from the client's system.
     * @param planId           The ID of the subscription plan in Stripe.
     * @param paymentMethodId  The ID of the payment method to be used for the subscription.
     * @param quantity         The quantity of the subscription, typically 1.
     * @return Subscription    The Stripe Subscription object that was created.
     * @throws StripeException If there is an issue communicating with the Stripe API.
     */
    public Subscription createSubscriptionWithPaymentMethodId(T userId, String planId, String paymentMethodId, int quantity) throws StripeException {
        String customerId = customerService.getOrCreateCustomer(userId).getId();

        Map<String, Object> subscriptionParams = new HashMap<>();
        subscriptionParams.put("customer", customerId);
        subscriptionParams.put("items", SubscriptionItemFactory.createSubscriptionItem(planId, quantity));
        subscriptionParams.put("default_payment_method", paymentMethodId);

        Subscription subscription = Subscription.create(subscriptionParams);
        StripeSubscription stripeSubscription = new StripeSubscriptionFactory()
                .create(customerId, subscription.getId(), planId, subscription.getStatus());
        subscriptionRepository.insert(stripeSubscription);

        return subscription;
    }
    
    /**
     * Creates a new subscription for a user with the specified plan and a new payment method token.
     *
     * @param userId   The ID of the user to create the subscription for.
     * @param planId   The ID of the subscription plan.
     * @param token    The payment method token.
     * @param quantity The quantity of the subscription, typically 1.
     * @return Subscription The created Stripe Subscription object.
     * @throws StripeException If there is an issue with the Stripe API call.
     */
    public Subscription createSubscriptionWithToken(T userId, String planId, String token, int quantity) throws StripeException {
        String paymentMethodId = customerService.addPaymentMethodToCustomer(userId, token).getId();
        return createSubscriptionWithPaymentMethodId(userId, planId, paymentMethodId, quantity);
    }

    /**
     * Applies a coupon code to an existing subscription in Stripe.
     *
     * @param subscriptionId The ID of the existing subscription in Stripe.
     * @param couponCode     The coupon code to apply to the subscription.
     * @return Subscription  The updated Stripe Subscription object.
     * @throws StripeException If there is an issue communicating with the Stripe API.
     */
    public Subscription applyCouponToSubscription(String subscriptionId, String couponCode) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);

        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCoupon(couponCode)
                .build();

        Subscription updatedSubscription = subscription.update(params);
        StripeSubscription foundStripeSubscription = subscriptionRepository.findBySubscriptionId(updatedSubscription.getId()).get();
        foundStripeSubscription.setStatus(updatedSubscription.getStatus());
        subscriptionRepository.update(foundStripeSubscription);

        return updatedSubscription;
    }

    /**
     * Cancels an existing subscription in Stripe at a specified date.
     *
     * @param subscriptionId The ID of the subscription to cancel.
     * @param cancelAt       The date at which the subscription should be cancelled.
     * @return Subscription  The updated Stripe Subscription object.
     * @throws StripeException If there is an issue communicating with the Stripe API.
     */
    public Subscription cancelSubscriptionAtDate(String subscriptionId, Instant cancelAt) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);

        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(false)
                .setCancelAt(cancelAt.getEpochSecond())
                .build();

        Subscription updatedSubscription = subscription.update(params);
        StripeSubscription foundStripeSubscription = subscriptionRepository.findBySubscriptionId(updatedSubscription.getId()).get();
        foundStripeSubscription.setStatus(updatedSubscription.getStatus());
        subscriptionRepository.update(foundStripeSubscription);

        return updatedSubscription;
    }

    /**
     * Schedules a subscription for cancellation at the end of the current billing period in Stripe.
     *
     * @param subscriptionId The ID of the subscription to cancel.
     * @return Subscription  The updated Stripe Subscription object.
     * @throws StripeException If there is an issue communicating with the Stripe API.
     */
    public Subscription cancelSubscriptionAtPeriodEnd(String subscriptionId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);

        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build();

        Subscription updatedSubscription = subscription.update(params);
         StripeSubscription foundStripeSubscription = subscriptionRepository.findBySubscriptionId(updatedSubscription.getId()).get();
        foundStripeSubscription.setStatus(updatedSubscription.getStatus());
        subscriptionRepository.update(foundStripeSubscription);

        return updatedSubscription;
    }
}