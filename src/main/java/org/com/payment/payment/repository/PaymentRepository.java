package org.com.payment.payment.repository;

import org.com.payment.enums.PaymentStatus;
import org.com.payment.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment,Long> {
    List<Payment> findByMerchantId(String merchantId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByCustomerEmail(String customerEmail);


    Optional<Payment> findByStripeTransactionId(String stripeTransactionId);
}

