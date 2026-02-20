package org.dbs.poc.unblu.controller;

import com.unblu.webapi.model.v3.AccountResult;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.service.UnbluService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/unblu")
@RequiredArgsConstructor
public class UnbluTestController {

    private final UnbluService unbluService;

    @GetMapping("/accounts")
    public ResponseEntity<AccountResult> getAccounts() {
        AccountResult accounts = unbluService.listAccounts();
        return ResponseEntity.ok(accounts);
    }
}
