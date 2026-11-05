package com.example.bank_transfer_app.dto;

import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor

public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            customerRepository.save(Customer.builder().accountNumber("100001").fullName("Kamal Silva").email("kamal@gmail.com").balance(new BigDecimal("50000.00")).build());
            customerRepository.save(Customer.builder().accountNumber("100002").fullName("Nimali Perera").email("nimali@gmail.com").balance(new BigDecimal("25000.00")).build());
            customerRepository.save(Customer.builder().accountNumber("100003").fullName("Saman Kumara").email("saman@gmail.com").balance(new BigDecimal("10000.00")).build());
        }
    }
}
