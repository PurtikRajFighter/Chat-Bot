package com.chatbot.controller;

import com.chatbot.config.DiscountConfig;
import com.chatbot.config.PackageConfig;
import com.chatbot.dto.PaymentRequest;
import com.chatbot.model.Transaction;
import com.chatbot.service.ExcelService;
import com.chatbot.service.TokenService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * PaymentController — Razorpay test payment integration.
 *
 * Packages and discount config are driven entirely from application.properties.
 * Add/remove/reorder packages there — no code changes needed here.
 *
 * Flow:
 *   1. Frontend calls GET  /api/payment/packages      → gets package list + discount info
 *   2. Frontend calls POST /api/payment/create-order  → creates Razorpay order
 *   3. Frontend opens Razorpay checkout modal
 *   4. Frontend calls POST /api/payment/verify        → verifies + credits tokens
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired private TokenService   tokenService;
    @Autowired private ExcelService   excelService;
    @Autowired private PackageConfig  packageConfig;
    @Autowired private DiscountConfig discountConfig;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // =========================================================================
    // GET /api/payment/packages
    // =========================================================================
    /**
     * Returns the full package list with live token counts (sale-adjusted if active).
     * The frontend uses this to build its UI — it never hardcodes packages itself.
     *
     * Response example (sale ON, 20% global + 50 extra on pkg[1]):
     * {
     *   "discountEnabled": true,
     *   "discountLabel":   "Diwali Sale",
     *   "globalBonusPercent": 20,
     *   "packages": [
     *     { "amount":10, "label":"Rs.10", "baseTokens":100, "bonusTokens":20,  "extraTokens":0,  "totalTokens":120, "popular":false },
     *     { "amount":25, "label":"Rs.25", "baseTokens":250, "bonusTokens":50,  "extraTokens":50, "totalTokens":350, "popular":true  },
     *     { "amount":50, "label":"Rs.50", "baseTokens":500, "bonusTokens":100, "extraTokens":150,"totalTokens":750, "popular":false }
     *   ]
     * }
     */
    @GetMapping("/packages")
    public ResponseEntity<Map<String, Object>> getPackages() {
        boolean saleOn      = discountConfig.isEnabled();
        int     bonusPct    = saleOn ? discountConfig.getGlobalBonusPercent() : 0;

        List<Map<String, Object>> pkgList = new ArrayList<>();
        List<PackageConfig.PackageItem> items = packageConfig.getPackages();

        for (int i = 0; i < items.size(); i++) {
            PackageConfig.PackageItem item = items.get(i);
            int base  = item.getBaseTokens();
            int bonus = saleOn ? (int) Math.round(base * bonusPct / 100.0) : 0;
            int extra = saleOn ? discountConfig.getExtraTokensFor(i) : 0;
            int total = base + bonus + extra;

            Map<String, Object> pkg = new LinkedHashMap<>();
            pkg.put("amount",      item.getAmount());
            pkg.put("label",       item.getLabel());
            pkg.put("baseTokens",  base);
            pkg.put("bonusTokens", bonus);
            pkg.put("extraTokens", extra);
            pkg.put("totalTokens", total);
            pkg.put("popular",     item.isPopular());
            pkgList.add(pkg);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("discountEnabled",     saleOn);
        response.put("discountLabel",       saleOn ? discountConfig.getLabel() : "");
        response.put("globalBonusPercent",  bonusPct);
        response.put("packages",            pkgList);
        return ResponseEntity.ok(response);
    }

    // Keep the old endpoint as an alias so nothing breaks
    @GetMapping("/discount-info")
    public ResponseEntity<Map<String, Object>> getDiscountInfo() {
        return getPackages();
    }

    // =========================================================================
    // POST /api/payment/create-order
    // =========================================================================
    /**
     * Creates a Razorpay order for the requested amount.
     * Token calculation (including any active discount) is done server-side.
     *
     * Request:  { "amount": 25 }
     * Response: { "success":true, "orderId":"order_xxx", "amount":2500,
     *             "currency":"INR", "keyId":"rzp_test_...", "tokensToAdd":350 }
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody PaymentRequest request,
            Authentication auth) {

        Map<String, Object> response = new HashMap<>();
        String userId = (String) auth.getPrincipal();

        if (request.getAmount() <= 0) {
            response.put("success", false);
            response.put("error", "Amount must be greater than 0");
            return ResponseEntity.badRequest().body(response);
        }

        // Find the matching package by amount
        List<PackageConfig.PackageItem> items = packageConfig.getPackages();
        int matchIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getAmount() == (int) request.getAmount()) {
                matchIndex = i;
                break;
            }
        }

        if (matchIndex == -1) {
            response.put("success", false);
            response.put("error", "No package found for amount ₹" + (int) request.getAmount()
                    + ". Valid amounts: " + items.stream()
                        .map(p -> String.valueOf(p.getAmount()))
                        .reduce((a, b) -> a + ", " + b).orElse("none"));
            return ResponseEntity.badRequest().body(response);
        }

        // Compute tokens — base + optional sale bonus
        PackageConfig.PackageItem item = items.get(matchIndex);
        int baseTokens  = item.getBaseTokens();
        int tokensToAdd = baseTokens;

        if (discountConfig.isEnabled()) {
            int bonus = (int) Math.round(baseTokens * discountConfig.getGlobalBonusPercent() / 100.0);
            int extra = discountConfig.getExtraTokensFor(matchIndex);
            tokensToAdd += bonus + extra;
            log.info("Sale active: base={}, bonus={}, extra={}, total={} tokens for Rs.{}",
                    baseTokens, bonus, extra, tokensToAdd, item.getAmount());
        }

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            int amountInPaise = item.getAmount() * 100; // Razorpay wants paise

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount",   amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt",  "receipt_" + UUID.randomUUID().toString().substring(0, 8));
            orderRequest.put("notes",    new JSONObject().put("userId", userId));

            Order  order   = client.orders.create(orderRequest);
            String orderId = order.get("id").toString();

            // Pre-save transaction with "created" status so verify() can find it
            excelService.saveTransaction(new Transaction(
                    UUID.randomUUID().toString(), userId,
                    request.getAmount(), tokensToAdd,
                    "created", LocalDateTime.now(), orderId
            ));

            response.put("success",         true);
            response.put("orderId",         orderId);
            response.put("amount",          amountInPaise);
            response.put("currency",        "INR");
            response.put("keyId",           razorpayKeyId);
            response.put("tokensToAdd",     tokensToAdd);
            response.put("baseTokens",      baseTokens);
            response.put("discountEnabled", discountConfig.isEnabled());
            response.put("discountLabel",   discountConfig.isEnabled() ? discountConfig.getLabel() : "");
            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            response.put("success", false);
            response.put("error",   "Payment service error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // =========================================================================
    // POST /api/payment/verify
    // =========================================================================
    /**
     * Verifies Razorpay payment signature and credits tokens to the user.
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @RequestBody PaymentRequest request,
            Authentication auth) {

        Map<String, Object> response = new HashMap<>();
        String userId = (String) auth.getPrincipal();

        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id",   request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature",  request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
            if (!isValid) {
                response.put("success", false);
                response.put("error",   "Payment verification failed. Invalid signature.");
                return ResponseEntity.status(400).body(response);
            }

            Optional<Transaction> optTx = excelService.findTransactionByOrderId(request.getRazorpayOrderId());
            if (optTx.isEmpty()) {
                response.put("success", false);
                response.put("error",   "Transaction not found for order: " + request.getRazorpayOrderId());
                return ResponseEntity.status(404).body(response);
            }

            Transaction tx = optTx.get();
            if ("paid".equals(tx.getStatus())) {
                response.put("success", false);
                response.put("error",   "Payment already processed");
                return ResponseEntity.badRequest().body(response);
            }

            tokenService.addTokens(userId, tx.getTokensAdded());
            tx.setStatus("paid");
            excelService.updateTransaction(tx);

            int newBalance = tokenService.getBalance(userId);
            log.info("Payment verified for user {}. Added {} tokens. New balance: {}",
                    userId, tx.getTokensAdded(), newBalance);

            response.put("success",     true);
            response.put("tokensAdded", tx.getTokensAdded());
            response.put("newBalance",  newBalance);
            response.put("message",     "Payment successful! " + tx.getTokensAdded() + " tokens added.");
            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            log.error("Payment verification error: {}", e.getMessage());
            response.put("success", false);
            response.put("error",   "Verification error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
