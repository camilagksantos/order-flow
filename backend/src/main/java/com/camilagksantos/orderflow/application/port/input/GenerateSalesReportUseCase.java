package com.camilagksantos.orderflow.application.port.input;

import java.time.LocalDate;

public interface GenerateSalesReportUseCase {
    byte[] generateSalesReport(LocalDate startDate, LocalDate endDate);
}