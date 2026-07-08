package com.camilagksantos.orderflow.application.port.input;

import java.time.LocalDate;

public interface GenerateSalesReportUseCase {
    byte[] execute(LocalDate startDate, LocalDate endDate);
}