package com.adarsh.zorvyn.Controllers;

import com.adarsh.zorvyn.Response.FinanceDashboardResponse;
import com.adarsh.zorvyn.Response.RecordResponse;
import com.adarsh.zorvyn.Service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DashboardController serves aggregated financial summary data
 * for the Finance Dashboard frontend.
 *
 * Access Control:
 *   ALL endpoints here are accessible to ALL authenticated roles:
 *   VIEWER, ANALYST, and ADMIN.
 *
 * Design Decision:
 *   VIEWERs are allowed here (but not in RecordController) because dashboard
 *   data is aggregated — it shows totals, trends, and summaries without exposing
 *   individual transaction-level records. This gives viewers meaningful read access
 *   without leaking sensitive financial details.
 *
 * Base path: /api/v1/dashboard
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    // Constructor injection for DashboardService.
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/v1/dashboard
     * Access: VIEWER, ANALYST, ADMIN
     *
     * Returns the full dashboard summary in a single response.
     * Designed for the initial page load — fetches all aggregated data at once
     * so the frontend can render the complete dashboard in one API call.
     *
     * Response includes:
     *   - totalIncome    : sum of all INCOME records
     *   - totalExpense   : sum of all EXPENSE records
     *   - netBalance     : totalIncome - totalExpense
     *   - categoryTotals : net amount per category (income positive, expense negative)
     *   - monthlyTrends  : net amount per month, sorted chronologically (yyyy-MM format)
     *   - recentTransactions : 5 most recent records by date
     *
     * Returns:
     *   200 OK - FinanceDashboardResponse
     */
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    @GetMapping
    public ResponseEntity<FinanceDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }

    /**
     * GET /api/v1/dashboard/summary/category
     * Access: VIEWER, ANALYST, ADMIN
     *
     * Returns net totals grouped by category.
     * Income contributes positively, expenses contribute negatively.
     *
     * Example response:
     *   { "Salary": 60000.0, "Rent": -18000.0, "Groceries": -8000.0 }
     *
     * Useful for rendering a category-breakdown chart on the frontend.
     *
     * Returns:
     *   200 OK - Map<String, Double> (category name → net amount)
     */
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    @GetMapping("/summary/category")
    public ResponseEntity<Map<String, Double>> getCategorySummary() {
        return ResponseEntity.ok(dashboardService.getCategorySummary());
    }

    /**
     * GET /api/v1/dashboard/summary/monthly
     * Access: VIEWER, ANALYST, ADMIN
     *
     * Returns net balance grouped by month in yyyy-MM format,
     * sorted chronologically using a TreeMap.
     *
     * Example response:
     *   { "2024-01": 12000.0, "2024-02": -3000.0, "2024-03": 9500.0 }
     *
     * Useful for rendering a monthly trend line chart on the frontend.
     *
     * Returns:
     *   200 OK - Map<String, Double> (yyyy-MM → net amount)
     */
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    @GetMapping("/summary/monthly")
    public ResponseEntity<Map<String, Double>> getMonthlySummary() {
        return ResponseEntity.ok(dashboardService.getMonthlySummary());
    }

    /**
     * GET /api/v1/dashboard/summary/recent
     * Access: VIEWER, ANALYST, ADMIN
     *
     * Returns the 5 most recent financial records sorted by date descending.
     * Designed for a "Recent Activity" widget on the dashboard.
     *
     * Note: This returns RecordResponse objects (not raw entities), so only
     * safe, pre-defined fields are exposed to the client.
     *
     * Returns:
     *   200 OK - List<RecordResponse> (max 5 items)
     */
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    @GetMapping("/summary/recent")
    public ResponseEntity<List<RecordResponse>> getRecentTransactions() {
        return ResponseEntity.ok(dashboardService.getRecentTransactions());
    }
}