package com.example.bank_transfer_app.controller;

import com.example.bank_transfer_app.dto.TransferRequest;
import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.service.BankService;
import com.example.bank_transfer_app.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;
    private final com.example.bank_transfer_app.repository.CustomerRepository customerRepository;
    private final com.example.bank_transfer_app.repository.BankTransactionRepository transactionRepository;

    // Dashboard: statistics, forms, customers table, latest transactions
    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        List<Customer> customers = (keyword != null && !keyword.trim().isEmpty())
                ? customerRepository.searchByName(keyword)
                : customerRepository.findAllOrdered();

        model.addAttribute("customers", customers);
        model.addAttribute("transactions", transactionRepository.findAllLatest());
        model.addAttribute("totalBalance", customerRepository.getTotalBalance());
        model.addAttribute("totalCustomers", customerRepository.countCustomersNative());
        model.addAttribute("successCount", transactionRepository.countByStatus(com.example.bank_transfer_app.model.TransactionStatus.SUCCESS));
        model.addAttribute("failCount", transactionRepository.countByStatus(com.example.bank_transfer_app.model.TransactionStatus.FAILED));
        model.addAttribute("transferRequest", new TransferRequest());

        return "bank";
    }

        // Add a customer (Form submission)
        @PostMapping("/customers")
        public String addCustomer(@RequestParam String accountNumber,
                @RequestParam String fullName,
                @RequestParam String email,
                @RequestParam BigDecimal balance,
                RedirectAttributes redirectAttributes) {
            try {
                bankService.createCustomer(accountNumber, fullName, email, balance);
                redirectAttributes.addFlashAttribute("successMessage", "Customer added successfully!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
            return "redirect:/";
        }

        // Add a customer (JSON submission via Postman)
        @PostMapping(value = "/customers", consumes = "application/json")
        public String addCustomerJson(@RequestBody Customer customer,
                RedirectAttributes redirectAttributes) {
            try {
                bankService.createCustomer(customer.getAccountNumber(), customer.getFullName(), customer.getEmail(), customer.getBalance());
                redirectAttributes.addFlashAttribute("successMessage", "Customer added successfully!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
            return "redirect:/";
        }

        // Transfer via form submission (Browser)
        @PostMapping("/transfer")
        public String transfer(@ModelAttribute("transferRequest") @Valid TransferRequest request,
                BindingResult bindingResult,
                RedirectAttributes redirectAttributes) {
            if (bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Validation failed. Please check input values.");
                return "redirect:/";
            }
            processTransfer(request, redirectAttributes);
            return "redirect:/";
        }

        // Transfer via JSON (Postman raw JSON body)
        @PostMapping(value = "/transfer", consumes = "application/json")
        public String transferJson(@RequestBody @Valid TransferRequest request,
                BindingResult bindingResult,
                RedirectAttributes redirectAttributes) {
            if (bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Validation failed. Please check input values.");
                return "redirect:/";
            }
            processTransfer(request, redirectAttributes);
            return "redirect:/";
        }

        private void processTransfer(TransferRequest request, RedirectAttributes redirectAttributes) {
            try {
                bankService.transfer(
                        request.getFromAccount(),
                        request.getToAccount(),
                        request.getAmount(),
                        request.isSimulateFailure()
                );
                redirectAttributes.addFlashAttribute("successMessage", "Transfer completed successfully!");
            } catch (Exception e) {
                // Record failure in a separate transaction (REQUIRES_NEW) so it doesn't get rolled back
                auditService.recordFailure(
                        request.getFromAccount(),
                        request.getToAccount(),
                        request.getAmount(),
                        e.getMessage()
                );
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
        }

        @GetMapping("/accounts/{accountNumber}/history")
        public String accountHistory(@PathVariable String accountNumber, Model model) {
            List<BankTransaction> history = transactionRepository.findHistory(accountNumber);
            BigDecimal totalSent = transactionRepository.getTotalAmountSent(accountNumber);
            model.addAttribute("accountNumber", accountNumber);
            model.addAttribute("history", history);
            model.addAttribute("totalSent", totalSent != null ? totalSent : BigDecimal.ZERO);
            return "history";
        }

        // Task 12: Show transfers above a given amount (default min=10000)
        @GetMapping("/large-transfers")
        public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
            model.addAttribute("minAmount", min);
            model.addAttribute("transactions", transactionRepository.findLargeTransfers(min));
            return "large-transfers";
        }

        // Task 12: Deposit
        @PostMapping("/deposit")
        public String deposit(@RequestParam String accountNumber,
                              @RequestParam BigDecimal amount,
                              RedirectAttributes redirectAttributes) {
            try {
                bankService.deposit(accountNumber, amount);
                redirectAttributes.addFlashAttribute("successMessage", "Deposit of LKR " + amount + " successful!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
            return "redirect:/";
        }

        // Task 12: Withdraw
        @PostMapping("/withdraw")
        public String withdraw(@RequestParam String accountNumber,
                               @RequestParam BigDecimal amount,
                               RedirectAttributes redirectAttributes) {
            try {
                bankService.withdraw(accountNumber, amount);
                redirectAttributes.addFlashAttribute("successMessage", "Withdrawal of LKR " + amount + " successful!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
            return "redirect:/";
        }

}
