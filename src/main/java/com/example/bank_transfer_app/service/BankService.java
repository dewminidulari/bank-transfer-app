package com.example.bank_transfer_app.service;

import com.example.bank_transfer_app.exception.AccountNotFoundException;
import com.example.bank_transfer_app.exception.BankException;
import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import com.example.bank_transfer_app.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {
    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    @Transactional(rollbackFor = Exception.class)
    public void transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {

        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver accounts must be different.");
        }
        if (customerRepository.getByAccountNumber(fromAccount).isEmpty() ||
                customerRepository.getByAccountNumber(toAccount).isEmpty()) {
            throw new AccountNotFoundException("One or both accounts do not exist.");
        }


        int updatedRows = customerRepository.debit(fromAccount, amount);
        if (updatedRows == 0) {
            throw new BankException("Insufficient balance in account " + fromAccount);
        }


        if (simulateFailure) {
            throw new RuntimeException("Simulated system failure after the withdrawal! All changes were rolled back.");
        }


        customerRepository.credit(toAccount, amount);


        BankTransaction tx = BankTransaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .description("Transfer completed successfully")
                .build();
        transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createCustomer(String accountNumber, String fullName, String email, BigDecimal balance) {
        if (customerRepository.countAccounts(accountNumber) > 0) {
            throw new BankException("Customer with account number " + accountNumber + " already exists.");
        }
        com.example.bank_transfer_app.model.Customer customer = com.example.bank_transfer_app.model.Customer.builder()
                .accountNumber(accountNumber)
                .fullName(fullName)
                .email(email)
                .balance(balance)
                .build();
        customerRepository.save(customer);
    }
}
