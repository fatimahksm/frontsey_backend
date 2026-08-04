package com.dbwb.platform.support;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.security.CurrentAccount;
import com.dbwb.platform.support.dto.SubmitSupportTicketRequest;
import com.dbwb.platform.support.dto.SupportTicketResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** BRD 9.15: contact-form support, for the authenticated account. Admin triage lives in the admin module. */
@RestController
@RequestMapping("/api/support/tickets")
public class SupportController {

    private final SupportService supportService;
    private final CurrentAccount currentAccount;

    public SupportController(SupportService supportService, CurrentAccount currentAccount) {
        this.supportService = supportService;
        this.currentAccount = currentAccount;
    }

    @PostMapping
    public ApiResponse<SupportTicketResponse> submit(@Valid @RequestBody SubmitSupportTicketRequest request) {
        var ticket = supportService.submit(currentAccount.get(), request);
        return ApiResponse.ok(SupportTicketResponse.from(ticket), "Support ticket submitted.");
    }

    @GetMapping
    public ApiResponse<List<SupportTicketResponse>> listMine() {
        return ApiResponse.ok(supportService.listMine(currentAccount.get())
                .stream().map(SupportTicketResponse::from).toList());
    }
}
