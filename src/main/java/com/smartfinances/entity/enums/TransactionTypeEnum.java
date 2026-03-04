package com.smartfinances.entity.enums;

public enum TransactionTypeEnum {
    DEBIT,      // Money out
    CREDIT,     // Money in
    TRANSFER,   // Between accounts
    ADJUSTMENT, // Manual correction entry
    CARRYOVER   // System-generated rollover — never created via public API
}

