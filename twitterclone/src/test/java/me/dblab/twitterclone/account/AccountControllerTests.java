package me.dblab.twitterclone.account;

import me.dblab.twitterclone.common.BaseControllerTest;
import me.dblab.twitterclone.config.jwt.TokenProvider;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.BDDAssertions.then;


public class AccountControllerTests extends BaseControllerTest {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TokenProvider tokenProvider;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ModelMapper modelMapper;

    private final String accountUrl = "/api/users";

    private final String BEARER = "Bearer ";

    @Test
    public void 유저_저장_테스트() {
        AccountDto accountDto = createAccountDto();

        webTestClient.post()
                .uri(accountUrl)
                .body(Mono.just(accountDto), AccountDto.class)
                .exchange()
                .expectStatus()
                .isCreated();

        Mono<Account> byEmail = accountRepository.findByEmail(accountDto.getEmail());

        StepVerifier.create(byEmail)
                .assertNext(account -> {
                    then(account.getUsername()).isEqualTo(appProperties.getTestUsername());
                    then(account.getNickname()).isEqualTo(appProperties.getTestNickname());
                    then(passwordEncoder.matches(appProperties.getTestPassword(), passwordEncoder.encode(account.getPassword())));
                    then(account.getEmail()).isEqualTo(appProperties.getTestEmail());
                })
                .verifyComplete();
    }

    @Test
    public void 유저_중복_테스트()   {
        AccountDto accountDto = createAccountDto();

        webTestClient.post()
                .uri(accountUrl)
                .body(Mono.just(accountDto), AccountDto.class)
                .exchange()
                .expectStatus()
                .isCreated();

        // 동일한 유저 등록시 BadRequest
        webTestClient.post()
                .uri(accountUrl)
                .body(Mono.just(accountDto), AccountDto.class)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    public void 유저_불러오기_테스트() {
        AccountDto accountDto = createAccountDto();

        webTestClient.post()
                .uri(accountUrl)
                .body(Mono.just(accountDto), AccountDto.class)
                .exchange()
                .expectStatus()
                .isCreated();

        Mono<Account> byEmail = accountRepository.findByEmail(accountDto.getEmail());
        Account account = byEmail.block();


        String jwt = BEARER + tokenProvider.generateToken(account);
        webTestClient.get()
                .uri(accountUrl + "/" + account.getId())
                .header(HttpHeaders.AUTHORIZATION, jwt)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void 유저_수정_테스트() {
        AccountDto accountDto = createAccountDto();

        webTestClient.post()
                .uri(accountUrl)
                .body(Mono.just(accountDto), AccountDto.class)
                .exchange()
                .expectStatus()
                .isCreated();

        Mono<Account> byEmail = accountRepository.findByEmail(accountDto.getEmail());
        Account account = byEmail.block();

        String jwt = BEARER + tokenProvider.generateToken(account);
        AccountDto updatedAccount = updateAccountDto();

        webTestClient.put()
                .uri(accountUrl + "/" + account.getId())
                .header(HttpHeaders.AUTHORIZATION, jwt)
                .body(Mono.just(updatedAccount), Account.class)
                .exchange()
                .expectStatus().isOk();

        Mono<Account> byId = accountRepository.findById(account.getId());
        Account modifiedAccount = byId.block();

        StepVerifier.create(byId)
                .assertNext(i -> {
                    then(modifiedAccount).isNotNull();
                    then(modifiedAccount.getId()).isEqualTo(account.getId());
                    then(modifiedAccount.getEmail()).isEqualTo(updatedAccount.getEmail());
                })
                .verifyComplete();
    }

    @Test
    public void 유저_삭제_테스트() {
        AccountDto accountDto = createAccountDto();

        webTestClient.post()
                .uri(accountUrl)
                .body(Mono.just(accountDto), AccountDto.class)
                .exchange()
                .expectStatus()
                .isCreated();

        Mono<Account> byEmail = accountRepository.findByEmail(accountDto.getEmail());
        Account account = byEmail.block();

        String jwt = BEARER + tokenProvider.generateToken(account);

        webTestClient.delete()
                .uri(accountUrl + "/" + account.getId())
                .header(HttpHeaders.AUTHORIZATION, jwt)
                .exchange()
                .expectStatus()
                .isOk();
    }

    private AccountDto updateAccountDto() {
        return AccountDto.builder()
                .username("modified" + appProperties.getTestUsername())
                .nickname("modified" + appProperties.getTestNickname())
                .password("modified" + appProperties.getTestPassword())
                .email("modified" + appProperties.getTestEmail())
                .build();
    }
}