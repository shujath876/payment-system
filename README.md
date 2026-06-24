
A production-style payment processing backend built with Spring Boot, integrating Stripe's Payment Intent API for secure card payments. The system handles the complete payment lifecycle — request validation, fraud detection, payment processing, asynchronous webhook confirmation, refunds, and email notifications — backed by PostgreSQL and deployed live on Railway.

Live Demo: https://payment-system-production-3c3e.up.railway.app/payment.html

Overview

This project simulates how a real merchant integrates payment processing into their platform — similar to how Swiggy, Zomato, or Amazon handle checkout. Rather than redirecting customers to an external payment page, card details are collected directly on the merchant's page using Stripe Elements, keeping the entire checkout experience on a single domain while remaining fully PCI-compliant (card data never touches the application server).

Built as a modular monolith — a single deployable Spring Boot application with package-level separation that mirrors future microservice boundaries (gateway, payment, provider, validation) — allowing clean extraction into independent services as the system scales.


Architecture

┌──────────────────────────────────────────────────────────────────┐
│                        Merchant Frontend                          │
│              (HTML + Stripe.js + Stripe Elements)                 │
└───────────────────────────────┬────────────────────────────────────┘
                                 │ REST API
                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│                     Payment Integration System                     │
│                                                                    │
│   ┌─────────────┐   ┌──────────────┐   ┌─────────────────────┐     │
│   │  Gateway    │──▶│   Payment    │──▶│  Provider           │    │
│   │  (planned)  │   │   Service    │   │  Integration Service │    │
│   └─────────────┘   └──────┬───────┘   └──────────┬───────────┘    │
│                            │                      │                │
│                     ┌──────▼───────┐               │                │
│                     │  Validation  │               │                │
│                     │   Service    │               │                │
│                     └──────────────┘               │                │
└─────────────────────────────────────────────────────┼────────────────┘
                                                       ▼
                                              ┌──────────────────┐
                                              │   Stripe API     │
                                              │  (PaymentIntent,  │
                                              │   Radar, Refunds) │
                                              └──────────────────┘
                            ▲
                            │ Webhook (async confirmation)
                            │
┌───────────────────────────┴───────────────────────────────────────┐
│                          PostgreSQL                               │
│            payments table — full transaction audit trail          │
└────────────────────────────────────────────────────────────────── ┘


Tech Stack

Language: Java 17
FrameworkSpring Boot 3.
xPersistenceSpring Data JPA, Hibernate.
Database :PostgreSQL
PaymentsStripe : Java SDK (PaymentIntents, Refunds, Radar, Webhooks)SecuritySpring 
Security (JWT — in progress)
FrontendHTML, JavaScript, Stripe Elements
Build : Maven
Deployment : Railway
EmailSpring Mail (Async, Gmail SMTP)


Payment Flow — Concrete Example

Scenario: A customer on FreshBasket, an online grocery store, buys items worth ₹850.

1. Customer fills checkout form (merchantId, email, amount, currency) on the
   merchant's page and enters card details into the embedded Stripe Elements
   iframe — card data never touches this application's servers.

2. POST /api/payments
   {
     "merchantId": "freshbasket_01",
     "customerEmail": "anil@gmail.com",
     "amount": 850.00,
     "currency": "INR",
     "description": "Grocery order #4421"
   }

3. Backend validates the request:
   - Structural validation (@Valid on DTO — required fields, email format)
   - Business validation (supported currency, amount within limits)
   - Fraud rules (velocity check, repeat-failure check, amount threshold)

4. Payment record saved to PostgreSQL with status = PENDING
   (saved BEFORE calling Stripe — so even a network failure to Stripe
   still leaves an auditable record of the attempt)

5. Backend creates a Stripe PaymentIntent — amount converted to paise
   (850.00 → 85000), returns { stripeClientSecret, stripeTransactionId }

6. Frontend uses stripe.confirmCardPayment(clientSecret, { card: cardElement })
   to confirm directly with Stripe — card details never pass through
   this backend.

7. Stripe processes the charge and fires a webhook:
   POST /api/webhooks/stripe   { "type": "payment_intent.succeeded", ... }

8. Webhook handler:
   - Verifies the HMAC signature (Stripe-Signature header) to confirm
     the request genuinely originated from Stripe
   - Extracts the PaymentIntent ID and looks up the matching payment
   - Reads the Stripe Radar risk score from the charge outcome
   - Updates status to SUCCESS (or FAILED, with a recorded reason)
   - Sends an async email receipt to the customer

9. Customer sees "Payment Successful" on screen within ~1-2 seconds —
   confirmed independently by Stripe, not by trusting the frontend.


Fraud Detection

A two-layer defence-in-depth approach:

Layer 1 — Rule-based checks (run before any Stripe API call)

Velocity check — blocks a merchant exceeding 5 payment attempts within 60 seconds
Repeat failure check — blocks a customer email after 5 consecutive failed payments
Amount threshold — flags/rejects transactions above a configured limit

Layer 2 — Stripe Radar (Stripe's ML-based risk engine)

Every confirmed charge returns a risk_score (0–100) and risk_level
Read from the webhook payload (charge.outcome) once the charge is processed
Scores are classified into LOW / MEDIUM / HIGH and persisted against the payment
HIGH risk transactions are automatically marked FAILED with the reason recorded


GET /api/payments/risk/high      → all transactions flagged HIGH risk
GET /api/payments/{id}/risk      → risk score + level for a specific payment
