package com.ecommerce.procurement.controller;

import com.ecommerce.procurement.service.ProcurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
@Tag(name = "Supplier & Procurement", description = "Supplier catalog, purchase orders, stock receiving, and invoice matching")
@SecurityRequirement(name = "bearerAuth")
public class ProcurementController {
    private final ProcurementService service;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        String actor = requireStaff(); return service.dashboard();
    }

    @GetMapping("/suppliers")
    public List<Map<String, Object>> suppliers(@RequestParam(defaultValue = "false") boolean includeArchived) {
        String actor = requireStaff(); return service.suppliers(includeArchived);
    }

    @PostMapping("/suppliers")
    public ResponseEntity<Map<String, Object>> createSupplier(@RequestBody Map<String, Object> request) {
        String actor = requireStaff(); return ResponseEntity.status(HttpStatus.CREATED).body(service.createSupplier(request, actor));
    }

    @PutMapping("/suppliers/{id}")
    public Map<String, Object> updateSupplier(@PathVariable long id, @RequestBody Map<String, Object> request) {
        String actor = requireStaff(); return service.updateSupplier(id, request, actor);
    }

    @DeleteMapping("/suppliers/{id}")
    public ResponseEntity<Void> archiveSupplier(@PathVariable long id) {
        String actor = requireStaff(); service.archiveSupplier(id, actor); return ResponseEntity.noContent().build();
    }

    @PatchMapping("/suppliers/{id}/restore")
    public Map<String, Object> restoreSupplier(@PathVariable long id) {
        String actor = requireStaff(); return service.restoreSupplier(id, actor);
    }

    @GetMapping("/suppliers/{supplierId}/products")
    public List<Map<String, Object>> supplierProducts(@PathVariable long supplierId,
                                                      @RequestParam(defaultValue = "false") boolean includeInactive) {
        String actor = requireStaff(); return service.supplierProducts(supplierId, includeInactive);
    }

    @PostMapping("/suppliers/{supplierId}/products")
    public ResponseEntity<Map<String, Object>> createSupplierProduct(@PathVariable long supplierId,
                                                                     @RequestBody Map<String, Object> request) {
        String actor = requireStaff(); return ResponseEntity.status(HttpStatus.CREATED).body(service.createSupplierProduct(supplierId, request, actor));
    }

    @PutMapping("/suppliers/{supplierId}/products/{productId}")
    public Map<String, Object> updateSupplierProduct(@PathVariable long supplierId, @PathVariable long productId,
                                                     @RequestBody Map<String, Object> request) {
        String actor = requireStaff(); return service.updateSupplierProduct(supplierId, productId, request, actor);
    }

    @DeleteMapping("/suppliers/{supplierId}/products/{productId}")
    public ResponseEntity<Void> archiveSupplierProduct(@PathVariable long supplierId, @PathVariable long productId) {
        String actor = requireStaff(); service.archiveSupplierProduct(supplierId, productId, actor); return ResponseEntity.noContent().build();
    }

    @GetMapping("/purchase-orders")
    public List<Map<String, Object>> purchaseOrders(@RequestParam(required = false) String status) {
        String actor = requireStaff(); return service.purchaseOrders(status);
    }

    @GetMapping("/purchase-orders/{id}")
    public Map<String, Object> purchaseOrder(@PathVariable long id) {
        String actor = requireStaff(); return service.purchaseOrder(id);
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<Map<String, Object>> createPurchaseOrder(@RequestBody Map<String, Object> request) {
        String actor = requireStaff(); return ResponseEntity.status(HttpStatus.CREATED).body(service.createPurchaseOrder(request, actor));
    }

    @PutMapping("/purchase-orders/{id}")
    public Map<String, Object> updatePurchaseOrder(@PathVariable long id, @RequestBody Map<String, Object> request) {
        String actor = requireStaff(); return service.updatePurchaseOrder(id, request, actor);
    }

    @PostMapping("/purchase-orders/{id}/approve")
    public Map<String, Object> approvePurchaseOrder(@PathVariable long id) {
        String actor = requireStaff(); return service.approvePurchaseOrder(id, actor);
    }

    @PostMapping("/purchase-orders/{id}/issue")
    public Map<String, Object> issuePurchaseOrder(@PathVariable long id) {
        String actor = requireStaff(); return service.issuePurchaseOrder(id, actor);
    }

    @PostMapping("/purchase-orders/{id}/cancel")
    public Map<String, Object> cancelPurchaseOrder(@PathVariable long id) {
        String actor = requireStaff(); return service.cancelPurchaseOrder(id, actor);
    }

    @PostMapping("/purchase-orders/{id}/receive")
    public Map<String, Object> receivePurchaseOrder(@PathVariable long id, @RequestBody Map<String, Object> request) {
        String actor = requireStaff(); return service.receivePurchaseOrder(id, request, actor);
    }

    @GetMapping("/supplier-invoices")
    public List<Map<String, Object>> supplierInvoices(@RequestParam(required = false) String status) {
        String actor = requireStaff(); return service.supplierInvoices(status);
    }

    @PostMapping("/purchase-orders/{id}/supplier-invoices")
    public ResponseEntity<Map<String, Object>> createSupplierInvoice(@PathVariable long id, @RequestBody Map<String, Object> request) {
        String actor = requireStaff(); return ResponseEntity.status(HttpStatus.CREATED).body(service.createSupplierInvoice(id, request, actor));
    }

    @GetMapping("/supplier-invoices/{id}")
    public Map<String, Object> supplierInvoice(@PathVariable long id) {
        String actor = requireStaff(); return service.supplierInvoice(id);
    }

    @PostMapping("/supplier-invoices/{id}/approve")
    public Map<String, Object> approveSupplierInvoice(@PathVariable long id) {
        String actor = requireStaff(); return service.approveSupplierInvoice(id, actor);
    }

    @GetMapping("/replenishment-suggestions")
    public List<Map<String, Object>> replenishmentSuggestions() {
        String actor = requireStaff(); return service.replenishmentSuggestions();
    }

    @PostMapping("/replenishment-suggestions/{productId}/draft-purchase-order")
    public ResponseEntity<Map<String, Object>> createDraftFromSuggestion(@PathVariable long productId,
                                                                         @RequestParam(required = false) Long supplierId) {
        String actor = requireStaff(); return ResponseEntity.status(HttpStatus.CREATED).body(service.createDraftFromSuggestion(productId, supplierId, actor));
    }

    /** Permission from the verified token (gateway rule): procurement:read for GET, procurement:manage otherwise. Returns the actor email. */
    private String requireStaff() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.ecommerce.procurement.config.TenantPrincipal p)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A tenant-scoped token is required.");
        }
        String method = ((org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest().getMethod();
        boolean read = "GET".equals(method);
        java.util.Set<String> perms = p.permissions();
        if (!perms.contains("*") && !perms.contains("procurement:manage") && !(read && perms.contains("procurement:read"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, (read ? "procurement:read" : "procurement:manage") + " permission required.");
        }
        return p.email();
    }
}
