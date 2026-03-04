package com.smartfinances.entity.enums;

public enum AllocationTypeEnum {
    BUDGET,       // Spending limit — balance counts down within interval
    SAVINGS,      // Saving toward a target — balance counts up within interval
    SINKING_FUND, // Saving for a specific future expense — requires targetDate
    INCOME        // Expected inflow — tracks received income within interval
}

