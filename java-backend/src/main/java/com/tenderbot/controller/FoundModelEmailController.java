package com.tenderbot.controller;

import com.tenderbot.dto.FoundModelEmailDto;
import com.tenderbot.dto.SendFoundModelEmailRequest;
import com.tenderbot.entity.FoundModel;
import com.tenderbot.entity.Tender;
import com.tenderbot.entity.TenderItem;
import com.tenderbot.repository.FoundModelRepository;
import com.tenderbot.service.FoundModelEmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/found-models")
public class FoundModelEmailController {

    private final FoundModelEmailService foundModelEmailService;
    private final FoundModelRepository foundModelRepository;

    public FoundModelEmailController(FoundModelEmailService foundModelEmailService,
                                     FoundModelRepository foundModelRepository) {
        this.foundModelEmailService = foundModelEmailService;
        this.foundModelRepository = foundModelRepository;
    }

    @GetMapping("/{foundModelId}/emails")
    public ResponseEntity<List<FoundModelEmailDto>> getEmails(@PathVariable Long foundModelId) {
        return ResponseEntity.ok(foundModelEmailService.getEmailsForFoundModel(foundModelId));
    }

    @GetMapping("/{foundModelId}/supplier-email")
    public ResponseEntity<String> resolveSupplierEmail(@PathVariable Long foundModelId) {
        return foundModelRepository.findById(foundModelId)
                .map(foundModelEmailService::resolveSupplierEmail)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{foundModelId}/emails/send")
    public ResponseEntity<FoundModelEmailDto> sendEmail(
            @PathVariable Long foundModelId,
            @RequestBody SendFoundModelEmailRequest request) {
        FoundModel foundModel = foundModelRepository.findById(foundModelId).orElse(null);
        if (foundModel == null) {
            return ResponseEntity.notFound().build();
        }
        TenderItem item = foundModel.getTenderItem();
        Tender tender = item != null ? item.getTender() : null;
        return ResponseEntity.ok(foundModelEmailService.sendEmail(
                foundModelId,
                request.toEmail(),
                request.subject(),
                request.body(),
                tender,
                item
        ));
    }

    @PostMapping("/emails/fetch-incoming")
    public ResponseEntity<Integer> fetchIncomingEmails() {
        return ResponseEntity.ok(foundModelEmailService.fetchIncomingEmails());
    }
}
