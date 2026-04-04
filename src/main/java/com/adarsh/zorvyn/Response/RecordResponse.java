package com.adarsh.zorvyn.Response;

import com.adarsh.zorvyn.Entity.Type;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * RecordResponse is the outbound DTO returned for all financial record operations.
 *
 * It is used as the response body for:
 *   - POST   /api/v1/records         (create a record)
 *   - GET    /api/v1/records         (paginated list of records)
 *   - PUT    /api/v1/records/{id}    (update a record)
 *   - GET    /api/v1/dashboard/summary/recent  (recent transactions)
 *
 * Design note:
 *   The raw Record entity is never sent directly to the client.
 *   This DTO deliberately excludes the 'createdBy' field (the User who created
 *   the record) to avoid leaking user account details in record responses.
 *   Only the fields the frontend actually needs are exposed here.
 *
 * Built using the Lombok @Builder pattern for clean, readable construction
 * in RecordsService and DashboardService.
 *
 * Example JSON response:
 * {
 *   "id": 1,
 *   "amount": 5000.00,
 *   "type": "INCOME",
 *   "category": "Salary",
 *   "date": "2024-06-15T00:00:00.000+00:00",
 *   "note": "Monthly salary credit"
 * }
 */
@Data
@Builder
public class RecordResponse {

    /**
     * Auto-generated unique identifier for the record.
     * Assigned by the database on creation.
     */
    private int id;

    /**
     * The monetary value of this record.
     * Always positive — the type field determines whether it is income or an expense.
     */
    private double amount;

    /**
     * The type of this financial entry.
     * Possible values: INCOME or EXPENSE
     *
     * Used by dashboard aggregations to calculate net balance:
     *   netBalance = sum(INCOME) - sum(EXPENSE)
     */
    private Type type;

    /**
     * The category this record belongs to (e.g., "Salary", "Rent", "Groceries").
     * Free-text — no fixed lookup table. Stored case-sensitively in the database.
     *
     * Used by the dashboard to group records in category-wise summaries.
     */
    private String category;

    /**
     * The date of this financial transaction.
     * Defaults to the date of creation if not provided during record creation.
     * On updates, the existing date is preserved unless a new date is explicitly sent.
     */
    private Date date;

    /**
     * An optional free-text description or note for this record.
     * May be null if no note was provided.
     * Also searchable via the 'search' query parameter on GET /api/v1/records.
     */
    private String note;

}