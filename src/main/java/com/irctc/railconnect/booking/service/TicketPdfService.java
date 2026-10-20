package com.irctc.railconnect.booking.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.irctc.railconnect.booking.dto.BookingResponse;
import com.irctc.railconnect.booking.dto.PassengerTicketDto;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TicketPdfService {

    private static final Logger log = LoggerFactory.getLogger(TicketPdfService.class);

    private static final Color COLOR_PRIMARY = new Color(15, 23, 42);       // Slate 900
    private static final Color COLOR_ACCENT = new Color(2, 132, 199);        // Sky 600
    private static final Color COLOR_BG_HEADER = new Color(30, 41, 59);     // Slate 800
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);    // Slate 50
    private static final Color COLOR_BORDER = new Color(226, 232, 240);      // Slate 200
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);      // Emerald 500

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_PRIMARY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139));
    private static final Font FONT_HEADER_WHITE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font FONT_SECTION_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_ACCENT);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(100, 116, 139));
    private static final Font FONT_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_PRIMARY);
    private static final Font FONT_CELL = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_PRIMARY);
    private static final Font FONT_CELL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_PRIMARY);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, new Color(148, 163, 184));

    public byte[] generateTicketPdf(BookingResponse booking) {
        log.info("Generating E-Ticket PDF for PNR: {}", booking.pnrNumber());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 28, 28, 24, 24);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Top Header with Logo, Title, and Verification QR Code
            PdfPTable headerTable = new PdfPTable(new float[]{3.2f, 1.2f});
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(8f);

            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            Paragraph orgTitle = new Paragraph("RAILCONNECT • INDIAN RAILWAYS", FONT_TITLE);
            Paragraph orgSub = new Paragraph("Electronic Reservation Slip (ERS) • Valid with Recognized Photo ID", FONT_SUBTITLE);
            Paragraph portalSub = new Paragraph("Booked via RailConnect High-Throughput Booking Engine", FONT_SUBTITLE);
            titleCell.addElement(orgTitle);
            titleCell.addElement(orgSub);
            titleCell.addElement(portalSub);
            headerTable.addCell(titleCell);

            // Generate and attach QR Code
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            try {
                byte[] qrBytes = generateQrCodeBytes(buildQrPayload(booking));
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.scaleToFit(75, 75);
                qrImage.setAlignment(Element.ALIGN_RIGHT);
                qrCell.addElement(qrImage);
            } catch (Exception ex) {
                log.warn("Failed to generate QR image for PNR {}: {}", booking.pnrNumber(), ex.getMessage());
            }
            headerTable.addCell(qrCell);
            document.add(headerTable);

            // 2. PNR, Train, & Booking Metadata Table
            PdfPTable metaTable = new PdfPTable(4);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(8f);

            addMetaField(metaTable, "PNR NUMBER", formatPnr(booking.pnrNumber()), true);
            addMetaField(metaTable, "TRAIN NO. & NAME", booking.trainNumber() + " - " + booking.trainName(), false);
            addMetaField(metaTable, "CLASS", booking.classCode(), false);
            addMetaField(metaTable, "QUOTA", booking.quotaType() != null ? booking.quotaType().name() : "GENERAL", false);

            addMetaField(metaTable, "JOURNEY DATE", booking.journeyDate() != null ? booking.journeyDate().toString() : "N/A", false);
            addMetaField(metaTable, "BOOKING STATUS", booking.bookingStatus() != null ? booking.bookingStatus().name() : "CONFIRMED", true);
            addMetaField(metaTable, "PAYMENT STATUS", booking.paymentStatus() != null ? booking.paymentStatus().name() : "SUCCESS", false);
            addMetaField(metaTable, "TRANSACTION REF", booking.transactionReference() != null ? booking.transactionReference() : "TXN-" + booking.pnrNumber(), false);

            document.add(metaTable);

            // 3. Journey Route Details Table
            PdfPTable journeyTable = new PdfPTable(4);
            journeyTable.setWidthPercentage(100);
            journeyTable.setSpacingAfter(8f);

            addMetaField(journeyTable, "FROM STATION", booking.fromStationCode(), true);
            addMetaField(journeyTable, "DEPARTURE TIME", booking.departureTime() != null ? booking.departureTime() : "--:--", false);
            addMetaField(journeyTable, "TO STATION", booking.toStationCode(), true);
            addMetaField(journeyTable, "ARRIVAL TIME", booking.arrivalTime() != null ? booking.arrivalTime() : "--:--", false);

            document.add(journeyTable);

            // 4. Passenger Details Table
            Paragraph passengerHeading = new Paragraph("PASSENGER DETAILS & SEAT ALLOCATION", FONT_SECTION_TITLE);
            passengerHeading.setSpacingAfter(4f);
            document.add(passengerHeading);

            PdfPTable passengerTable = new PdfPTable(new float[]{0.6f, 3.2f, 1.4f, 1.8f, 2.5f, 2.5f});
            passengerTable.setWidthPercentage(100);
            passengerTable.setSpacingAfter(8f);

            addTableHeader(passengerTable, "#");
            addTableHeader(passengerTable, "Passenger Name");
            addTableHeader(passengerTable, "Age / Sex");
            addTableHeader(passengerTable, "Berth Pref");
            addTableHeader(passengerTable, "Booking Status");
            addTableHeader(passengerTable, "Current Live Status");

            List<PassengerTicketDto> passengers = booking.passengers();
            if (passengers != null && !passengers.isEmpty()) {
                int index = 1;
                for (PassengerTicketDto p : passengers) {
                    boolean alternate = (index % 2 == 0);
                    Color rowBg = alternate ? COLOR_BG_LIGHT : Color.WHITE;

                    addTableCell(passengerTable, String.valueOf(index), rowBg, Element.ALIGN_CENTER, FONT_CELL);
                    addTableCell(passengerTable, p.passengerName(), rowBg, Element.ALIGN_LEFT, FONT_CELL_BOLD);
                    addTableCell(passengerTable, p.age() + " / " + (p.gender() != null ? p.gender().name() : "M"), rowBg, Element.ALIGN_CENTER, FONT_CELL);
                    addTableCell(passengerTable, p.berthPreference() != null ? p.berthPreference().name() : "NO_PREF", rowBg, Element.ALIGN_CENTER, FONT_CELL);
                    addTableCell(passengerTable, p.bookingStatusDetails() != null ? p.bookingStatusDetails() : (p.bookingStatus() != null ? p.bookingStatus().name() : "CNF"), rowBg, Element.ALIGN_CENTER, FONT_CELL);

                    String currentAlloc = (p.currentCoach() != null && p.currentSeatNumber() != null)
                            ? p.currentCoach() + " - " + p.currentSeatNumber() + " (" + (p.currentBerthType() != null ? p.currentBerthType().name() : "BERTH") + ")"
                            : (p.currentStatusDetails() != null ? p.currentStatusDetails() : (p.currentStatus() != null ? p.currentStatus().name() : "CNF"));

                    addTableCell(passengerTable, currentAlloc, rowBg, Element.ALIGN_CENTER, FONT_CELL_BOLD);
                    index++;
                }
            } else {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No passenger details available", FONT_CELL));
                emptyCell.setColspan(6);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(8f);
                passengerTable.addCell(emptyCell);
            }
            document.add(passengerTable);

            // 5. Payment & Fare Summary Box
            PdfPTable fareTable = new PdfPTable(new float[]{3.5f, 1.5f});
            fareTable.setWidthPercentage(100);
            fareTable.setSpacingAfter(8f);

            PdfPCell fareLabelCell = new PdfPCell(new Phrase("TOTAL FARE PAID (Inclusive of GST & IRCTC Service Charges)", FONT_CELL_BOLD));
            fareLabelCell.setBackgroundColor(COLOR_BG_LIGHT);
            fareLabelCell.setPadding(6f);
            fareLabelCell.setBorderColor(COLOR_BORDER);
            fareTable.addCell(fareLabelCell);

            PdfPCell fareValueCell = new PdfPCell(new Phrase("Rs. " + (booking.totalFare() != null ? booking.totalFare().toPlainString() : "0.00"), FONT_TITLE));
            fareValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            fareValueCell.setBackgroundColor(COLOR_BG_LIGHT);
            fareValueCell.setPadding(6f);
            fareValueCell.setBorderColor(COLOR_BORDER);
            fareTable.addCell(fareValueCell);

            document.add(fareTable);

            // 6. Important Passenger Instructions
            Paragraph instructionTitle = new Paragraph("IMPORTANT INSTRUCTIONS FOR PASSENGERS", FONT_SECTION_TITLE);
            instructionTitle.setSpacingAfter(3f);
            document.add(instructionTitle);

            String[] rules = {
                    "1. Original Photo ID proof (Aadhaar, Passport, Driving License, Voter Card, or Govt ID) must be carried by at least one passenger.",
                    "2. Virtual Electronic Reservation Slip (ERS) shown on mobile phone/laptop is accepted as valid ticket with ID proof.",
                    "3. In case of train cancellation or RAC/WL charting status, refund is automatically credited to the source payment method.",
                    "4. Contact Railway Customer Care Helpline: 139 for live train status, charting, and emergency security assistance."
            };

            for (String rule : rules) {
                Paragraph p = new Paragraph(rule, FONT_SUBTITLE);
                p.setLeading(11f);
                document.add(p);
            }

            // 7. Footer
            Paragraph footer = new Paragraph("\nSave Paper, Save Trees • Digital ERS is accepted by TTE on board • RailConnect Next-Gen Ticketing", FONT_FOOTER);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to render E-Ticket PDF for PNR {}: {}", booking.pnrNumber(), ex.getMessage(), ex);
            throw new IllegalStateException("Could not generate E-Ticket PDF: " + ex.getMessage(), ex);
        }
    }

    private void addMetaField(PdfPTable table, String label, String value, boolean highlight) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5f);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBackgroundColor(highlight ? new Color(240, 249, 255) : COLOR_BG_LIGHT);

        Paragraph pLabel = new Paragraph(label, FONT_LABEL);
        pLabel.setLeading(9f);
        cell.addElement(pLabel);

        Paragraph pVal = new Paragraph(value != null ? value : "-", highlight ? FONT_SECTION_TITLE : FONT_VALUE);
        pVal.setLeading(12f);
        cell.addElement(pVal);

        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER_WHITE));
        cell.setBackgroundColor(COLOR_BG_HEADER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(COLOR_BG_HEADER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Color bgColor, int align, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        cell.setBorderColor(COLOR_BORDER);
        table.addCell(cell);
    }

    private String buildQrPayload(BookingResponse b) {
        return "RAILCONNECT-TICKET-VERIFICATION\n" +
                "PNR: " + b.pnrNumber() + "\n" +
                "TRAIN: " + b.trainNumber() + " - " + b.trainName() + "\n" +
                "DATE: " + b.journeyDate() + "\n" +
                "ROUTE: " + b.fromStationCode() + " -> " + b.toStationCode() + "\n" +
                "CLASS: " + b.classCode() + " | QUOTA: " + b.quotaType() + "\n" +
                "STATUS: " + b.bookingStatus() + "\n" +
                "FARE: INR " + (b.totalFare() != null ? b.totalFare().toPlainString() : "0.00") + "\n" +
                "PASSENGERS: " + (b.passengers() != null ? b.passengers().size() : 0);
    }

    private byte[] generateQrCodeBytes(String payload) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(payload, BarcodeFormat.QR_CODE, 200, 200);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "PNG", baos);
        return baos.toByteArray();
    }

    private String formatPnr(String pnr) {
        if (pnr == null || pnr.length() != 10) return pnr;
        return pnr.substring(0, 3) + "-" + pnr.substring(3, 6) + "-" + pnr.substring(6);
    }
}
