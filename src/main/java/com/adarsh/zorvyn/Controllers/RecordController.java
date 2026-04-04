package com.adarsh.zorvyn.Controllers;

import com.adarsh.zorvyn.Entity.Type;
import com.adarsh.zorvyn.Entity.User;
import com.adarsh.zorvyn.Request.RecordRequest;
import com.adarsh.zorvyn.Response.RecordResponse;
import com.adarsh.zorvyn.Service.RecordsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.Date;

/**
 * RecordController handles all financial record operations.
 *
 * Financial records represent individual income or expense entries.
 * Each record stores: amount, type (INCOME/EXPENSE), category, date, and an optional note.
 *
 * Access Control:
 *   - ADMIN    : full access (create, read, update, delete)
 *   - ANALYST  : read-only access (filtered + paginated listing)
 *   - VIEWER   : no access to raw records — they can only view aggregated dashboard data
 *
 * Design Decision:
 *   VIEWERs are excluded from this controller because raw records may expose
 *   sensitive transaction-level data. Dashboard endpoints serve aggregated
 *   summaries which are safe to share with all roles.
 *
 * Base path: /api/v1/records
 */
@RestController
@RequestMapping("/api/v1/records")
public class RecordController {

    @Autowired
    private RecordsService recordsService;

    /**
     * POST /api/v1/records
     * Access: ADMIN only
     *
     * Creates a new financial record. The currently authenticated admin user
     * is captured via @AuthenticationPrincipal and stored as the createdBy field.
     *
     * If no date is provided in the request body, it defaults to today's date.
     *
     * @Valid triggers Jakarta validation: amount must be positive,
     * type and category are required.
     *
     * Returns:
     *   200 OK - RecordResponse with generated id and all fields
     *   400    - Validation errors (missing type, category, negative amount, etc.)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RecordResponse> createRecord(
            @Valid @RequestBody RecordRequest recordRequest,
            @AuthenticationPrincipal User user  // injected from the validated JWT token
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recordsService.createRecord(recordRequest, user));
    }

    /**
     * GET /api/v1/records
     * Access: ANALYST and ADMIN
     *
     * Returns a paginated list of financial records with optional filters.
     * All filter parameters are optional and can be freely combined.
     *
     * Query Parameters:
     *   type     - Filter by INCOME or EXPENSE
     *   category - Case-insensitive exact match on category name
     *   from     - Start of date range (format: yyyy-MM-dd)
     *   to       - End of date range (format: yyyy-MM-dd)
     *   search   - Keyword search across note and category fields
     *   page     - Page number, zero-based (default: 0)
     *   size     - Number of records per page (default: 10)
     *   sort     - Sort field (default: date)
     *
     * Filtering is done in the database via a single JPQL query in RecordsRepository,
     * not in memory — this is efficient even with large datasets and pagination.
     *
     * Returns:
     *   200 OK - Paginated Page<RecordResponse> with metadata (totalElements, totalPages, etc.)
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<RecordResponse>> getRecords(
            @RequestParam(required = false) Type type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date to,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "date") Pageable pageable
    ) {
        return ResponseEntity.ok(recordsService.getFilteredRecords(type, category, from, to, search, pageable));
    }

    /**
     * PUT /api/v1/records/{id}
     * Access: ADMIN only
     *
     * Fully updates an existing financial record by its ID.
     * All fields in the request body are applied.
     *
     * Date handling:
     *   If a date is provided, it overwrites the existing date.
     *   If date is omitted, the record's current date is preserved unchanged.
     *   This prevents accidental date resets on partial updates.
     *
     * Returns:
     *   200 OK - Updated RecordResponse
     *   400    - Validation errors
     *   404    - Record not found
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<RecordResponse> updateRecord(
            @PathVariable int id,
            @Valid @RequestBody RecordRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(recordsService.updateRecord(id, request, user));
    }

    /**
     * DELETE /api/v1/records/{id}
     * Access: ADMIN only
     *
     * Permanently deletes a financial record by its ID.
     * This is a hard delete — the record is removed from the database.
     *
     * Design note: Unlike users (which are soft-deleted), records are hard-deleted
     * because the assignment did not specify a compliance/retention requirement.
     * In a production system, a soft-delete flag would be preferred.
     *
     * Returns:
     *   204 No Content - Record deleted successfully
     *   404            - Record not found
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable int id
    ) {
        recordsService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}