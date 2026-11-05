package com.example.bank_transfer_app.repository;

import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long>{

    @Query("SELECT t FROM BankTransaction t ORDER BY t.createdAt DESC")
    List<BankTransaction> findAllLatest();


    @Query("SELECT t FROM BankTransaction t WHERE t.fromAccount = :acc OR t.toAccount = :acc ORDER BY t.createdAt DESC")
    List<BankTransaction> findHistory(@Param("acc") String accountNumber);


    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);


    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    // Task 12: Returns transfers above a given amount
    @Query("SELECT t FROM BankTransaction t WHERE t.amount >= :minAmount ORDER BY t.createdAt DESC")
    List<BankTransaction> findLargeTransfers(@Param("minAmount") BigDecimal minAmount);

    // Task 12: Total amount sent by an account (SUM of SUCCESS transfers)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM BankTransaction t WHERE t.fromAccount = :account AND t.status = com.example.bank_transfer_app.model.TransactionStatus.SUCCESS")
    BigDecimal getTotalAmountSent(@Param("account") String account);
}
