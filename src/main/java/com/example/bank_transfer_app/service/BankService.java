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


        BigDecimal fee = amount.multiply(new BigDecimal("0.01")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);


        if (customerRepository.countAccounts("999999") == 0) {
            customerRepository.save(com.example.bank_transfer_app.model.Customer.builder()
                    .accountNumber("999999")
                    .fullName("Bank Fee Reserve Account")
                    .email("fees@bank.com")
                    .balance(BigDecimal.ZERO)
                    .build());
        }


        int updatedRows = customerRepository.debit(fromAccount, totalDebit);
        if (updatedRows == 0) {
            throw new BankException("Insufficient balance in account " + fromAccount + " (including 1% fee of LKR " + fee + ")");
        }


        if (simulateFailure) {
            throw new RuntimeException("Simulated system failure after the withdrawal! All changes were rolled back.");
        }


        customerRepository.credit(toAccount, amount);


        customerRepository.credit("999999", fee);

        BankTransaction tx = BankTransaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .description("Transfer completed (1% fee of LKR " + fee + " paid to 999999)")
                .build();
        transactionRepository.save(tx);
    }


    @Transactional(rollbackFor = Exception.class)
    public void deposit(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Deposit amount must be greater than zero.");
        }
        if (customerRepository.getByAccountNumber(accountNumber).isEmpty()) {
            throw new AccountNotFoundException("Account " + accountNumber + " does not exist.");
        }
        customerRepository.credit(accountNumber, amount);
        BankTransaction tx = BankTransaction.builder()
                .fromAccount("CASH_DEPOSIT")
                .toAccount(accountNumber)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .description("Cash deposit")
                .build();
        transactionRepository.save(tx);
    }


    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Withdrawal amount must be greater than zero.");
        }
        if (customerRepository.getByAccountNumber(accountNumber).isEmpty()) {
            throw new AccountNotFoundException("Account " + accountNumber + " does not exist.");
        }
        int updatedRows = customerRepository.debit(accountNumber, amount);
        if (updatedRows == 0) {
            throw new BankException("Insufficient balance in account " + accountNumber + " for withdrawal.");
        }
        BankTransaction tx = BankTransaction.builder()
                .fromAccount(accountNumber)
                .toAccount("CASH_WITHDRAWAL")
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .description("Cash withdrawal")
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
