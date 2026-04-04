package com.adarsh.zorvyn.Response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * FinanceDashboardResponse is the outbound DTO returned by the main dashboard endpoint.
 *
 * Returned by:
 *   GET /api/v1/dashboard
 *
 * This single response bundles all the summary data needed to render a complete
 * finance dashboard in one API call — avoiding multiple round trips from the frontend.
 *
 * It is composed of:
 *   - Scalar totals  : totalIncome, totalExpense, netBalance
 *   - Grouped maps   : categoryTotals, monthlyTrends
 *   - Recent records : recentTransactions (last 5 by date)
 *
 * Access: all authenticated roles (VIEWER, ANALYST, ADMIN)
 * Reason: data here is aggregated — no raw transaction details are exposed
 * directly, making it safe for all roles including viewers.
 *
 * Example JSON response:
 * {
 *   "totalIncome": 85000.00,
 *   "totalExpense": 42000.00,
 *   "netBalance": 43000.00,
 *   "categoryTotals": {
 *     "Salary": 60000.00,
 *     "Rent": -18000.00,
 *     "Groceries": -8000.00
 *   },
 *   "monthlyTrends": {
 *     "2024-01": 12000.00,
 *     "2024-02": -3000.00
 *   },
 *   "recentTransactions": [ ... ]
 * }
 */
@Data
public class FinanceDashboardResponse {

    /**
     * Sum of the amounts of all INCOME type records in the system.
     * Always a non-negative value.
     */
    private double totalIncome;

    /**
     * Sum of the amounts of all EXPENSE type records in the system.
     * Always a non-negative value. Subtracted from totalIncome to get netBalance.
     */
    private double totalExpense;

    /**
     * The net financial position: totalIncome - totalExpense.
     * Positive value means more income than expenses (surplus).
     * Negative value means more expenses than income (deficit).
     */
    private double netBalance;

    /**
     * A map of category name → net amount for that category.
     * Calculated as: sum of INCOME records - sum of EXPENSE records per category.
     *
     * INCOME entries contribute positively, EXPENSE entries negatively.
     * A negative value means the category is net-expense (e.g., "Rent": -18000.0).
     * A positive value means the category is net-income (e.g., "Salary": 60000.0).
     *
     * Useful for rendering a category breakdown chart on the frontend.
     *
     * Example: { "Salary": 60000.0, "Rent": -18000.0, "Groceries": -8000.0 }
     */
    private Map<String, Double> categoryTotals;

    /**
     * A map of month (yyyy-MM format) → net amount for that month.
     * Calculated the same way as categoryTotals but grouped by month.
     *
     * Sorted chronologically (backed by a TreeMap in DashboardService)
     * so the frontend can render a time-series trend chart directly.
     *
     * Example: { "2024-01": 12000.0, "2024-02": -3000.0, "2024-03": 9500.0 }
     */
    private Map<String, Double> monthlyTrends;

    /**
     * The 5 most recent financial records sorted by date descending.
     * Designed for a "Recent Activity" widget on the dashboard.
     *
     * Returns RecordResponse objects (not raw entities), so only
     * safe pre-defined fields are exposed — the createdBy user is excluded.
     *
     * Will contain fewer than 5 entries if the total number of records is less than 5.
     */
    private List<RecordResponse> recentTransactions;

}