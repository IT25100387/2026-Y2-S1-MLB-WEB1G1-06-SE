package com.fuelstation.controller;

import com.fuelstation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReceiptRestController {
    @Autowired private BillingService billing;
    @Autowired private PdfReceiptService pdf;
    private ResponseEntity<byte[]> response(byte[] bytes,String name) { return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+name+".pdf\"").body(bytes); }
    @GetMapping("/api/billing/invoices/{id}/receipt") public ResponseEntity<byte[]> invoice(@PathVariable Long id) { var invoice=billing.getInvoiceById(id).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));return response(pdf.generateInvoicePdf(invoice,billing.getPaymentsByReferenceNumber(invoice.getInvoiceNumber())),invoice.getInvoiceNumber()); }
    @GetMapping("/api/v1/customer/invoices/{number}/receipt") public ResponseEntity<byte[]> ownInvoice(@PathVariable String number,Authentication auth) { var invoice=billing.getInvoiceForCustomer(number,auth.getName()).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Invoice is not yours"));return response(pdf.generateInvoicePdf(invoice,billing.getPaymentsByReferenceNumber(number)),invoice.getInvoiceNumber()); }

    @GetMapping("/customer/invoices/{invoiceNumber}/pdf")
    public ResponseEntity<byte[]> downloadLegacyInvoicePdf(@PathVariable String invoiceNumber, Authentication auth) {
        String username = auth.getName();
        java.util.Optional<com.fuelstation.model.Invoice> invOpt = billing.getInvoiceForCustomer(invoiceNumber, username);

        if (invOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        com.fuelstation.model.Invoice invoice = invOpt.get();
        java.util.List<com.fuelstation.model.PaymentRecord> payments = billing.getPaymentsByReferenceNumber(invoice.getInvoiceNumber());

        byte[] pdfBytes = pdf.generateInvoicePdf(invoice, payments);
        String fileName = "Invoice_" + invoice.getInvoiceNumber() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentLength(pdfBytes.length);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
