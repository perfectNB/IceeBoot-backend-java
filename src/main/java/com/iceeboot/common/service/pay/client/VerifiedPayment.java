package com.iceeboot.common.service.pay.client;

public record VerifiedPayment(String orderNo, String transactionId, int amountFen) {}
