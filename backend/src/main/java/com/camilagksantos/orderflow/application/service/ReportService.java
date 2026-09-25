package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.GenerateSalesReportUseCase;
import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.shared.Money;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService implements GenerateSalesReportUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public byte[] generateSalesReport(LocalDate startDate, LocalDate endDate) {
        List<ShopOrder> orders = orderRepositoryPort.findByCreatedAtBetween(startDate, endDate);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle currencyStyle = buildCurrencyStyle(workbook);

            buildOrdersSheet(workbook, orders, headerStyle, currencyStyle);
            buildSummarySheet(workbook, orders, headerStyle, currencyStyle);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate sales report", e);
        }
    }

    private void buildOrdersSheet(XSSFWorkbook workbook, List<ShopOrder> orders,
                                  CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Orders");
        String[] headers = {"Order Number", "Customer Email", "Status", "Created At",
                "Subtotal", "Shipping Cost", "Discount", "Total Amount", "Payment Method"};

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int rowIndex = 1;
        for (ShopOrder order : orders) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(order.getOrderNumber());
            row.createCell(1).setCellValue(order.getCustomerEmail());
            row.createCell(2).setCellValue(order.getStatus().name());
            row.createCell(3).setCellValue(
                    order.getCreatedAt() != null ? order.getCreatedAt().format(dateFormatter) : "");
            setCurrencyCell(row.createCell(4), order.getSubtotal(), currencyStyle);
            setCurrencyCell(row.createCell(5), order.getShippingCost(), currencyStyle);
            setCurrencyCell(row.createCell(6), order.getDiscountAmount(), currencyStyle);
            setCurrencyCell(row.createCell(7), order.getTotalAmount(), currencyStyle);
            row.createCell(8).setCellValue(order.getPaymentMethod().name());
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void buildSummarySheet(XSSFWorkbook workbook, List<ShopOrder> orders,
                                   CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Summary");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"Status", "Order Count", "Total Amount"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        Map<OrderStatus, Long> countsByStatus = new EnumMap<>(OrderStatus.class);
        Map<OrderStatus, Money> totalsByStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            countsByStatus.put(status, 0L);
            totalsByStatus.put(status, Money.zero());
        }

        for (ShopOrder order : orders) {
            OrderStatus status = order.getStatus();
            countsByStatus.merge(status, 1L, Long::sum);
            totalsByStatus.put(status, totalsByStatus.get(status).add(order.getTotalAmount()));
        }

        int rowIndex = 1;
        for (OrderStatus status : OrderStatus.values()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(status.name());
            row.createCell(1).setCellValue(countsByStatus.get(status));
            setCurrencyCell(row.createCell(2), totalsByStatus.get(status), currencyStyle);
        }

        Row totalRow = sheet.createRow(rowIndex);
        totalRow.createCell(0).setCellValue("TOTAL");
        totalRow.createCell(1).setCellValue(orders.size());
        Money grandTotal = orders.stream()
                .map(ShopOrder::getTotalAmount)
                .reduce(Money.zero(), Money::add);
        setCurrencyCell(totalRow.createCell(2), grandTotal, currencyStyle);

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void setCurrencyCell(Cell cell, Money money, CellStyle currencyStyle) {
        cell.setCellValue(money.amount().doubleValue());
        cell.setCellStyle(currencyStyle);
    }

    private CellStyle buildHeaderStyle(XSSFWorkbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        return style;
    }

    private CellStyle buildCurrencyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00 \"EUR\""));
        return style;
    }
}