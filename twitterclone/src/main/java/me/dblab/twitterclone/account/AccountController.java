package me.dblab.twitterclone.account;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class AccountController {

    private final AccountService accountService;
    private final AccountValidator accountValidator;

    public AccountController(AccountService accountService, AccountValidator accountValidator) {
        this.accountService = accountService;
        this.accountValidator = accountValidator;
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Account> getAccount(@PathVariable String id) {
        return accountService.getAccount(id);
    }

    @PostMapping
    public Mono<ResponseEntity> saveAccount(@RequestBody AccountDto accountDto)  {
        this.validate(accountDto);
        return accountService.saveAccount(accountDto);
    }

    private void validate(AccountDto accountDto) {
        Errors errors = new BeanPropertyBindingResult(accountDto, "account");
        log.info("controller validate");
        accountValidator.validate(accountDto, errors);
        if (errors.hasErrors()) {
            log.info("controller validate errors");
            throw new ServerWebInputException(errors.toString());
        }
    }

    @PostMapping("/login")
    public Mono<ResponseEntity> login(@RequestBody Mono<Account> account) {
        log.info("controller login");
        return accountService.login(account);
    }

    // Update User
    @PutMapping("/{id}")
    public Mono<ResponseEntity> updateAccount(@PathVariable String id, @RequestBody AccountDto accountDto) {
        this.validate(accountDto);
        return accountService.updateAccount(id, accountDto);
    }

    // Delete User
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> deleteAccount(@PathVariable String id)  {
        return accountService.deleteAccount(id);
    }
}

