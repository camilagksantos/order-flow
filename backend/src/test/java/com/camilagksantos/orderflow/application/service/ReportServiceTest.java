package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.shared.Money;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @InjectMocks
    private ReportService reportService;

    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2026, 1, 1);
        endDate = LocalDate.of(2026, 1, 31);
    }

    private ShopOrder buildOrder(OrderStatus status, BigDecimal totalAmount) {
        return ShopOrder.builder()
                .id(UUID.randomUUID().toString())
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customerId(1L)
                .customerEmail("customer@example.com")
                .status(status)
                .items(List.of())
                .subtotal(Money.of(totalAmount))
                .shippingCost(Money.zero())
                .discountAmount(Money.zero())
                .totalAmount(Money.of(totalAmount))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();
    }

    @Test
    void shouldGenerateWorkbookWithTwoSheets() throws IOException {
        when(orderRepositoryPort.findByCreatedAtBetween(startDate, endDate))
                .thenReturn(List.of(buildOrder(OrderStatus.PAID, BigDecimal.valueOf(100))));

        byte[] result = reportService.generateSalesReport(startDate, endDate);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("Orders");
            assertThat(workbook.getSheetName(1)).isEqualTo("Summary");
        }
    }

    @Test
    void shouldListEachOrderAsARowInOrdersSheet() throws IOException {
        ShopOrder order = buildOrder(OrderStatus.PAID, BigDecimal.valueOf(150));
        when(orderRepositoryPort.findByCreatedAtBetween(startDate, endDate))
                .thenReturn(List.of(order));

        byte[] result = reportService.generateSalesReport(startDate, endDate);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet ordersSheet = workbook.getSheet("Orders");
            Row headerRow = ordersSheet.getRow(0);
            Row dataRow = ordersSheet.getRow(1);

            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Order Number");
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo(order.getOrderNumber());
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("customer@example.com");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("PAID");
            assertThat(dataRow.getCell(7).getNumericCellValue()).isEqualTo(150.0);
        }
    }

    @Test
    void shouldAggregateOrdersByStatusInSummarySheet() throws IOException {
        List<ShopOrder> orders = List.of(
                buildOrder(OrderStatus.PAID, BigDecimal.valueOf(100)),
                buildOrder(OrderStatus.PAID, BigDecimal.valueOf(200)),
                buildOrder(OrderStatus.CANCELLED, BigDecimal.valueOf(50))
        );
        when(orderRepositoryPort.findByCreatedAtBetween(startDate, endDate)).thenReturn(orders);

        byte[] result = reportService.generateSalesReport(startDate, endDate);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet summarySheet = workbook.getSheet("Summary");

            int paidRowIndex = OrderStatus.PAID.ordinal() + 1;
            Row paidRow = summarySheet.getRow(paidRowIndex);
            assertThat(paidRow.getCell(0).getStringCellValue()).isEqualTo("PAID");
            assertThat(paidRow.getCell(1).getNumericCellValue()).isEqualTo(2.0);
            assertThat(paidRow.getCell(2).getNumericCellValue()).isEqualTo(300.0);

            int cancelledRowIndex = OrderStatus.CANCELLED.ordinal() + 1;
            Row cancelledRow = summarySheet.getRow(cancelledRowIndex);
            assertThat(cancelledRow.getCell(1).getNumericCellValue()).isEqualTo(1.0);
            assertThat(cancelledRow.getCell(2).getNumericCellValue()).isEqualTo(50.0);
        }
    }

    @Test
    void shouldIncludeTotalRowInSummarySheet() throws IOException {
        List<ShopOrder> orders = List.of(
                buildOrder(OrderStatus.PAID, BigDecimal.valueOf(100)),
                buildOrder(OrderStatus.CANCELLED, BigDecimal.valueOf(50))
        );
        when(orderRepositoryPort.findByCreatedAtBetween(startDate, endDate)).thenReturn(orders);

        byte[] result = reportService.generateSalesReport(startDate, endDate);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet summarySheet = workbook.getSheet("Summary");
            int totalRowIndex = OrderStatus.values().length + 1;
            Row totalRow = summarySheet.getRow(totalRowIndex);

            assertThat(totalRow.getCell(0).getStringCellValue()).isEqualTo("TOTAL");
            assertThat(totalRow.getCell(1).getNumericCellValue()).isEqualTo(2.0);
            assertThat(totalRow.getCell(2).getNumericCellValue()).isEqualTo(150.0);
        }
    }

    @Test
    void shouldGenerateEmptyWorkbookWhenNoOrdersInRange() throws IOException {
        when(orderRepositoryPort.findByCreatedAtBetween(startDate, endDate))
                .thenReturn(List.of());

        byte[] result = reportService.generateSalesReport(startDate, endDate);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet ordersSheet = workbook.getSheet("Orders");
            assertThat(ordersSheet.getLastRowNum()).isEqualTo(0);

            Sheet summarySheet = workbook.getSheet("Summary");
            int totalRowIndex = OrderStatus.values().length + 1;
            Row totalRow = summarySheet.getRow(totalRowIndex);
            assertThat(totalRow.getCell(1).getNumericCellValue()).isEqualTo(0.0);
        }
    }
}