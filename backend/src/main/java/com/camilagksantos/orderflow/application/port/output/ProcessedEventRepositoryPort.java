package com.camilagksantos.orderflow.application.port.output;

public interface ProcessedEventRepositoryPort {

    boolean existsById(String id);
    void save(String id);
}
