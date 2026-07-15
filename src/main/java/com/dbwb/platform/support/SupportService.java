package com.dbwb.platform.support;

import com.dbwb.platform.common.config.SupportProperties;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.support.dto.SubmitSupportTicketRequest;
import com.dbwb.platform.support.entity.SupportTicket;
import com.dbwb.platform.support.entity.SupportTicketStatus;
import com.dbwb.platform.support.repository.SupportTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** BRD 9.15: contact-form support. */
@Service
public class SupportService {

    private final SupportTicketRepository repository;
    private final EmailService emailService;
    private final SupportProperties supportProperties;

    public SupportService(SupportTicketRepository repository, EmailService emailService, SupportProperties supportProperties) {
        this.repository = repository;
        this.emailService = emailService;
        this.supportProperties = supportProperties;
    }

    @Transactional
    public SupportTicket submit(AuthenticatedAccount caller, SubmitSupportTicketRequest request) {
        SupportTicket ticket = new SupportTicket();
        ticket.setSubmitterAccountId(caller.accountId());
        ticket.setCategory(request.category());
        ticket.setSubject(request.subject());
        ticket.setMessage(request.message());
        ticket.setAttachmentUrl(request.attachmentUrl());
        ticket.setStatus(SupportTicketStatus.OPEN);
        repository.save(ticket);

        if (supportProperties.getNotificationEmail() != null && !supportProperties.getNotificationEmail().isBlank()) {
            emailService.send(supportProperties.getNotificationEmail(),
                    "New support ticket: " + request.subject(),
                    "From account " + caller.email() + " (" + request.category() + "):\n\n" + request.message());
        }
        return ticket;
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> listMine(AuthenticatedAccount caller) {
        return repository.findBySubmitterAccountIdOrderByCreatedAtDesc(caller.accountId());
    }

    /** Super Admin only - see the admin module for the authorization check on this call site. */
    @Transactional(readOnly = true)
    public List<SupportTicket> listAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public SupportTicket updateStatus(UUID ticketId, SupportTicketStatus status) {
        SupportTicket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found."));
        ticket.setStatus(status);
        return ticket;
    }
}
