package com.camilagksantos.orderflow.infrastructure.adapter.output.email;

import com.camilagksantos.orderflow.application.port.output.EmailNotificationPort;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailEmailAdapter implements EmailNotificationPort {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendOrderConfirmation(ShopOrder order) {
        send(
                order.getCustomerEmail(),
                "Order Confirmed — " + order.getOrderNumber(),
                "Your order " + order.getOrderNumber() + " has been confirmed. Total: " + order.getTotalAmount().amount() + " EUR"
        );
        log.info("Order confirmation email sent for order: {}", order.getOrderNumber());
    }

    @Override
    public void sendOrderShipped(ShopOrder order) {
        send(
                order.getCustomerEmail(),
                "Order Shipped — " + order.getOrderNumber(),
                "Your order " + order.getOrderNumber() + " has been shipped. Tracking: " + order.getTrackingCode()
        );
        log.info("Order shipped email sent for order: {}", order.getOrderNumber());
    }

    @Override
    public void sendOrderCancelled(ShopOrder order) {
        send(
                order.getCustomerEmail(),
                "Order Cancelled — " + order.getOrderNumber(),
                "Your order " + order.getOrderNumber() + " has been cancelled. Reason: " + order.getCancelReason()
        );
        log.info("Order cancelled email sent for order: {}", order.getOrderNumber());
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}