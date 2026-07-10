package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.GenerateSalesReportUseCase;
import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService implements GenerateSalesReportUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public byte[] generateSalesReport(LocalDate startDate, LocalDate endDate) {
        return new byte[0];
    }
}