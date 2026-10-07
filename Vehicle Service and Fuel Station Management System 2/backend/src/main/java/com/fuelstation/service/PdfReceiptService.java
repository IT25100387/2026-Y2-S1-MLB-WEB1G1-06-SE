package com.fuelstation.service;

import com.fuelstation.model.Invoice;
import com.fuelstation.model.PaymentRecord;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import com.fuelstation.model.JobCard;
import com.fuelstation.repository.JobCardRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PdfReceiptService {

    @Autowired
    private JobCardRepository jobCardRepository;

    private static final Color GOLD_ACCENT = new Color(217, 119, 6); // #D97706
    private static final Color CHARCOAL = new Color(30, 41, 59);     // #1E293B
    private static final Color LIGHT_BG = new Color(248, 250, 252);   // #F8FAFC
    private static final Color BORDER_COLOR = new Color(226, 232, 240); // #E2E8F0
    private static final Color GREEN_STATUS = new Color(22, 101, 52); // #166534

    public byte[] generateInvoicePdf(Invoice invoice, java.util.List<PaymentRecord> payments) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(229,46,46));
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, CHARCOAL);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, CHARCOAL);
            document.add(new Paragraph("FUELCORE - INVOICE", heading));
            document.add(new Paragraph("Invoice number: " + invoice.getInvoiceNumber(), bold));
            if (invoice.getReferenceNumber() != null) document.add(new Paragraph("Service reference: " + invoice.getReferenceNumber(), normal));
            document.add(new Paragraph("Customer: " + invoice.getCustomerName() + " | Vehicle: " + (invoice.getLicensePlate() == null ? "-" : invoice.getLicensePlate()), normal));
            document.add(new Paragraph("Issued: " + invoice.getInvoiceDate() + " | Due: " + (invoice.getDueDate() == null ? "After completion" : invoice.getDueDate()) + " | State: " + invoice.getStatus(), normal));
            if ("SERVICE".equals(invoice.getInvoiceType()) && !Boolean.TRUE.equals(invoice.getFinalized())) document.add(new Paragraph("Provisional service invoice: used parts may change the total. Full settlement and daily charges start after completion.", normal));
            document.add(new Paragraph(" "));
            PdfPTable items = new PdfPTable(4);
            items.setWidthPercentage(100); items.setWidths(new float[]{4,1,2,2});
            for (String label : new String[]{"Item", "Quantity", "Unit price (LKR)", "Amount (LKR)"}) addTableCell(items,label,bold,LIGHT_BG,Element.ALIGN_LEFT);
            for (var item : invoice.getLineItems()) {
                addTableCell(items,item.getItemName(),normal,Color.WHITE,Element.ALIGN_LEFT);
                addTableCell(items,String.valueOf(item.getQuantity()),normal,Color.WHITE,Element.ALIGN_RIGHT);
                addTableCell(items,String.format("%,.2f",item.getUnitPrice()),normal,Color.WHITE,Element.ALIGN_RIGHT);
                addTableCell(items,String.format("%,.2f",item.getTotalAmount()),normal,Color.WHITE,Element.ALIGN_RIGHT);
            }
            document.add(items);
            PdfPTable totals = new PdfPTable(2); totals.setWidthPercentage(60); totals.setHorizontalAlignment(Element.ALIGN_RIGHT); totals.setSpacingBefore(15);
            String[] labels = {"Subtotal", "Discount", "Convenience fee", "Daily interest", "Overdue charges", "Total", "Payments retained", "Refunded", "Balance due"};
            Double[] values = {invoice.getGrossTotalAmount(), invoice.getDiscountAmount(), invoice.getConvenienceFee(), invoice.getPreOverdueInterest(), invoice.getOverduePenalty(), invoice.getNetTotalWithPenalty(), invoice.getAmountPaid(), invoice.getRefundedAmount(), invoice.getBalanceDue()};
            for (int i=0; i<labels.length; i++) addSummaryRow(totals,labels[i],String.format("LKR %,.2f", values[i] == null ? 0 : values[i]),bold,i==5||i==8);
            document.add(totals);
            if (payments != null && !payments.isEmpty()) {
                document.add(new Paragraph("Payment history",bold));
                PdfPTable ledger = new PdfPTable(5); ledger.setWidthPercentage(100);
                for (String label : new String[]{"Payment", "Date", "Method", "Paid LKR", "Refunded LKR"}) addTableCell(ledger,label,bold,LIGHT_BG,Element.ALIGN_LEFT);
                for (var payment : payments) {
                    addTableCell(ledger,"PAY-"+payment.getId(),normal,Color.WHITE,Element.ALIGN_LEFT);
                    addTableCell(ledger,String.valueOf(payment.getPaymentDate()),normal,Color.WHITE,Element.ALIGN_LEFT);
                    addTableCell(ledger,payment.getPaymentMethod(),normal,Color.WHITE,Element.ALIGN_LEFT);
                    addTableCell(ledger,String.format("%,.2f",payment.getAmount()),normal,Color.WHITE,Element.ALIGN_RIGHT);
                    addTableCell(ledger,String.format("%,.2f",payment.getRefundedAmount()==null?0:payment.getRefundedAmount()),normal,Color.WHITE,Element.ALIGN_RIGHT);
                }
                document.add(ledger);
            }
            document.close();
            return out.toByteArray();
        } catch (Exception e) { document.close(); throw new IllegalStateException("Unable to create invoice PDF",e); }
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font, boolean isTotal) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(3);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(3);
        if (isTotal) {
            labelCell.setBorder(Rectangle.TOP);
            valueCell.setBorder(Rectangle.TOP);
            labelCell.setBorderColor(BORDER_COLOR);
            valueCell.setBorderColor(BORDER_COLOR);
            labelCell.setPaddingTop(6);
            valueCell.setPaddingTop(6);
        }
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }
}
