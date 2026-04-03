package ru.neo.study.dealapi;

import dto.*;
import enums.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import ru.neo.study.dealapi.builders.ClientBuilder;
import ru.neo.study.dealapi.builders.CreditBuilder;
import ru.neo.study.dealapi.builders.ScoringDataBuilder;
import ru.neo.study.dealapi.builders.StatementAndStatusHistoryBuilder;
import ru.neo.study.dealapi.calculatorClient.CalculatorClient;
import ru.neo.study.dealapi.calculatorClient.CalculatorClientConfig;
import ru.neo.study.dealapi.controller.DealApiController;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Credit;
import ru.neo.study.dealapi.entity.Passport;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.repository.ClientRepository;
import ru.neo.study.dealapi.repository.CreditRepository;
import ru.neo.study.dealapi.repository.StatementRepository;
import ru.neo.study.dealapi.service.DealApiService;
import ru.neo.study.dealapi.service.DealApiServiceImpl;
import ru.neo.study.dealapi.service.compositeServices.CalculateLoanTermsService;
import ru.neo.study.dealapi.service.compositeServices.FinishRegistrationAndCalculateService;
import ru.neo.study.dealapi.service.compositeServices.SelectOfferService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class DealApiApplicationTests {
    private static UUID sharedStatementId;

    @Mock
    private StatementRepository statementRepository;
    @Mock
    private DealApiService dealApiService;
    @Mock
    private CalculateLoanTermsService calculateLoanTermsService;
    @Mock
    private SelectOfferService selectOfferService;
    @Mock
    private FinishRegistrationAndCalculateService finishRegistrationAndCalculateService;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private CalculatorClient calculatorClient;
    @Mock
    private ClientBuilder clientBuilderMock;
    @Mock
    private StatementAndStatusHistoryBuilder statementAndStatusHistoryBuilderMock;
    @Mock
    private ScoringDataBuilder scoringDataBuilderMock;
    @Mock
    private CreditRepository creditRepository;
    @Mock
    private CreditBuilder creditBuilderMock;

    private ClientBuilder clientBuilder;
    private CreditBuilder creditBuilder;
    private ScoringDataBuilder scoringDataBuilder;
    private StatementAndStatusHistoryBuilder statementAndStatusHistoryBuilder;
    private CalculatorClientConfig calculatorClientConfig;
    private DealApiController controller;
    private DealApiServiceImpl dealApiServiceImpl;
    private CalculateLoanTermsService calculateLoanTermsServiceImpl;
    private FinishRegistrationAndCalculateService finishRegistrationAndCalculateServiceImpl;
    private SelectOfferService selectOfferServiceImpl;

    @BeforeAll
    static void beforeAll() {
        sharedStatementId = UUID.randomUUID();
    }

    @BeforeEach
    void beforeEach() {
        clientBuilder = new ClientBuilder();
        creditBuilder = new CreditBuilder();
        scoringDataBuilder = new ScoringDataBuilder();
        statementAndStatusHistoryBuilder = new StatementAndStatusHistoryBuilder(statementRepository);
        calculatorClientConfig = new CalculatorClientConfig();

        controller = new DealApiController(dealApiService);
        dealApiServiceImpl = new DealApiServiceImpl(
                calculateLoanTermsService,
                selectOfferService,
                finishRegistrationAndCalculateService
        );

        calculateLoanTermsServiceImpl = new CalculateLoanTermsService(
                clientRepository,
                statementRepository,
                calculatorClient,
                clientBuilderMock,
                statementAndStatusHistoryBuilderMock
        );

        finishRegistrationAndCalculateServiceImpl = new FinishRegistrationAndCalculateService(
                clientBuilderMock,
                clientRepository,
                scoringDataBuilderMock,
                statementAndStatusHistoryBuilderMock,
                calculatorClient,
                creditRepository,
                statementRepository,
                creditBuilderMock
        );

        selectOfferServiceImpl = new SelectOfferService(
                statementAndStatusHistoryBuilderMock,
                statementRepository
        );
    }

    @Test
    void shouldBuildClientFromLoanStatementRequest() {
        LoanStatementRequestDto request = loanStatementRequest();

        Client client = clientBuilder.buildClient(request);

        assertEquals(request.getFirstName(), client.getFirstName());
        assertEquals(request.getLastName(), client.getLastName());
        assertEquals(request.getMiddleName(), client.getMiddleName());
        assertEquals(request.getEmail(), client.getEmail());
        assertEquals(request.getBirthdate(), client.getBirthDate());
        assertNotNull(client.getPassport());
        assertEquals(request.getPassportSeries(), client.getPassport().getSeries());
        assertEquals(request.getPassportNumber(), client.getPassport().getNumber());
    }

    @Test
    void shouldEnrichClientAndCreateEmploymentAndPassportWhenNeeded() {
        Client client = Client.builder().passport(null).build();
        FinishRegistrationRequestDto request = finishRegistrationRequest();

        clientBuilder.enrichClient(client, request);

        assertEquals(request.getGender(), client.getGender());
        assertEquals(request.getMaritalStatus(), client.getMaritalStatus());
        assertEquals(request.getDependentAmount(), client.getDependentAmount());
        assertEquals(request.getAccountNumber(), client.getAccountNumber());
        assertNotNull(client.getPassport());
        assertEquals(request.getPassportIssueDate(), client.getPassport().getIssueDate());
        assertEquals(request.getPassportIssueBranch(), client.getPassport().getIssueBranch());
        assertNotNull(client.getEmployment());
        assertEquals(request.getEmployment().getEmployerINN(), client.getEmployment().getEmployerInn());
        assertEquals(request.getEmployment().getEmploymentStatus(), client.getEmployment().getStatus());
    }

    @Test
    void shouldReuseExistingPassportAndSkipEmploymentWhenItIsMissing() {
        Passport passport = Passport.builder().series("1111").number("222222").build();
        Client client = Client.builder().passport(passport).build();
        FinishRegistrationRequestDto request = new FinishRegistrationRequestDto(
                finishRegistrationRequest().getGender(),
                finishRegistrationRequest().getMaritalStatus(),
                1,
                finishRegistrationRequest().getPassportIssueDate(),
                "RUS-001",
                null,
                "40702810900000000001"
        );

        clientBuilder.enrichClient(client, request);

        assertSame(passport, client.getPassport());
        assertEquals("RUS-001", client.getPassport().getIssueBranch());
        assertNull(client.getEmployment());
    }

    @Test
    void shouldBuildCreditEntityFromDto() {
        CreditDto creditDto = creditDto();

        Credit credit = creditBuilder.buildCredit(creditDto);

        assertEquals(creditDto.getAmount(), credit.getAmount());
        assertEquals(creditDto.getTerm(), credit.getTerm());
        assertEquals(creditDto.getMonthlyPayment(), credit.getMonthlyPayment());
        assertEquals(creditDto.getRate(), credit.getRate());
        assertEquals(creditDto.getPsk(), credit.getPsk());
        assertEquals(creditDto.getPaymentSchedule(), credit.getPaymentSchedule());
        assertEquals(creditDto.getIsInsuranceEnabled(), credit.getInsuranceEnabled());
        assertEquals(creditDto.getIsSalaryClient(), credit.getSalaryClient());
        assertEquals(CreditStatus.CALCULATED, credit.getCreditStatus());
    }

    @Test
    void shouldBuildScoringDataFromStatementAndRequest() {
        Client client = client();
        LoanOfferDto offerDto = loanOffer(sharedStatementId, "16.0");
        Statement statement = Statement.builder()
                .id(sharedStatementId)
                .client(client)
                .appliedOffer(offerDto)
                .build();
        FinishRegistrationRequestDto request = finishRegistrationRequest();

        ScoringDataDto scoringDataDto = scoringDataBuilder.buildScoringData(statement, request);

        assertEquals(offerDto.getRequestedAmount(), scoringDataDto.getAmount());
        assertEquals(offerDto.getTerm(), scoringDataDto.getTerm());
        assertEquals(client.getFirstName(), scoringDataDto.getFirstName());
        assertEquals(client.getPassport().getSeries(), scoringDataDto.getPassportSeries());
        assertEquals(request.getPassportIssueDate(), scoringDataDto.getPassportIssueDate());
        assertEquals(request.getEmployment(), scoringDataDto.getEmployment());
        assertEquals(offerDto.getIsInsuranceEnabled(), scoringDataDto.getIsInsuranceEnabled());
        assertEquals(offerDto.getIsSalaryClient(), scoringDataDto.getIsSalaryClient());
    }

    @Test
    void shouldThrowWhenAppliedOfferIsMissing() {
        Statement statement = Statement.builder()
                .id(UUID.randomUUID())
                .client(client())
                .appliedOffer(null)
                .build();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scoringDataBuilder.buildScoringData(statement, finishRegistrationRequest())
        );

        assertEquals("Не выбрано кредитное предложение", exception.getMessage());
    }

    @Test
    void shouldReturnStatementById() {
        UUID statementId = UUID.randomUUID();
        Statement statement = Statement.builder().id(statementId).build();
        when(statementRepository.findById(statementId)).thenReturn(Optional.of(statement));

        Statement result = statementAndStatusHistoryBuilder.getStatementById(statementId);

        assertSame(statement, result);
    }

    @Test
    void shouldThrowWhenStatementNotFound() {
        UUID statementId = UUID.randomUUID();
        when(statementRepository.findById(statementId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> statementAndStatusHistoryBuilder.getStatementById(statementId)
        );

        assertTrue(exception.getMessage().contains(statementId.toString()));
    }

    @Test
    void shouldBuildHistoryItem() {
        StatementStatusHistoryDto item =
                statementAndStatusHistoryBuilder.buildHistoryItem(ApplicationStatus.APPROVED, ChangeType.MANUAL);

        assertEquals(ApplicationStatus.APPROVED, item.getStatus());
        assertEquals(ChangeType.MANUAL, item.getChangeType());
        assertNotNull(item.getTime());
    }

    @Test
    void shouldAppendHistoryWhenListIsNull() {
        Statement statement = Statement.builder().statusHistory(null).build();

        statementAndStatusHistoryBuilder.appendStatusHistory(
                statement,
                ApplicationStatus.PREAPPROVAL,
                ChangeType.AUTOMATIC
        );

        assertNotNull(statement.getStatusHistory());
        assertEquals(1, statement.getStatusHistory().size());
        assertEquals(ApplicationStatus.PREAPPROVAL, statement.getStatusHistory().getFirst().getStatus());
    }

    @Test
    void shouldAppendHistoryToExistingList() {
        Statement statement = Statement.builder()
                .statusHistory(List.of(new StatementStatusHistoryDto(
                        ApplicationStatus.APPROVED,
                        null,
                        ChangeType.MANUAL
                )))
                .build();

        statementAndStatusHistoryBuilder.appendStatusHistory(
                statement,
                ApplicationStatus.CC_APPROVED,
                ChangeType.AUTOMATIC
        );

        assertEquals(2, statement.getStatusHistory().size());
        assertEquals(ApplicationStatus.CC_APPROVED, statement.getStatusHistory().get(1).getStatus());
    }

    @Test
    void shouldCreateRestClientBean() {
        RestClient client = calculatorClientConfig.calculatorRestClient("http://localhost:8081");
        assertNotNull(client);
    }

    @Test
    void shouldReturnCreatedWithOffersOnCalculateLoanTerms() {
        LoanStatementRequestDto request = loanStatementRequest();
        List<LoanOfferDto> offers = List.of(loanOffer(UUID.randomUUID(), "15.0"));
        when(dealApiService.calculateLoanTerms(request)).thenReturn(offers);

        ResponseEntity<List<LoanOfferDto>> response = controller.calculateLoanTerms(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(offers, response.getBody());
    }

    @Test
    void shouldDelegateSelectOfferFromController() {
        LoanOfferDto offerDto = loanOffer(UUID.randomUUID(), "14.0");

        controller.selectOffer(offerDto);

        verify(dealApiService).selectOffer(offerDto);
    }

    @Test
    void shouldDelegateFinishRegistrationAndCalculateFromController() {
        UUID statementId = UUID.randomUUID();
        FinishRegistrationRequestDto request = finishRegistrationRequest();

        controller.finishRegistrationAndCalculate(statementId, request);

        verify(dealApiService).finishRegistrationAndCalculate(statementId, request);
    }

    @Test
    void shouldDelegateCalculateLoanTermsFromServiceImpl() {
        LoanStatementRequestDto request = loanStatementRequest();
        List<LoanOfferDto> offers = List.of(loanOffer(UUID.randomUUID(), "11.0"));
        when(calculateLoanTermsService.calculateLoanTerms(request)).thenReturn(offers);

        List<LoanOfferDto> result = dealApiServiceImpl.calculateLoanTerms(request);

        assertEquals(offers, result);
    }

    @Test
    void shouldDelegateSelectOfferFromServiceImpl() {
        LoanOfferDto offerDto = loanOffer(UUID.randomUUID(), "12.0");

        dealApiServiceImpl.selectOffer(offerDto);

        verify(selectOfferService).selectOffer(offerDto);
    }

    @Test
    void shouldDelegateFinishRegistrationAndCalculateFromServiceImpl() {
        UUID statementId = UUID.randomUUID();
        FinishRegistrationRequestDto request = finishRegistrationRequest();

        dealApiServiceImpl.finishRegistrationAndCalculate(statementId, request);

        verify(finishRegistrationAndCalculateService).finishRegistrationAndCalculate(statementId, request);
    }

    @Test
    void shouldCreateClientStatementAndReturnSortedOffersWithStatementIds() {
        LoanStatementRequestDto request = loanStatementRequest();
        Client builtClient = client();
        UUID statementId = UUID.randomUUID();
        Statement savedStatement = Statement.builder()
                .id(statementId)
                .client(builtClient)
                .status(ApplicationStatus.PREAPPROVAL)
                .creationDate(LocalDateTime.now())
                .build();
        LoanOfferDto lowRateOffer = loanOffer(null, "12.0");
        LoanOfferDto highRateOffer = loanOffer(null, "18.0");

        when(clientBuilderMock.buildClient(request)).thenReturn(builtClient);
        when(clientRepository.save(builtClient)).thenReturn(builtClient);
        when(statementAndStatusHistoryBuilderMock.buildHistoryItem(any(), any())).thenCallRealMethod();
        when(statementRepository.save(any(Statement.class))).thenReturn(savedStatement);
        when(calculatorClient.getOffers(request)).thenReturn(List.of(lowRateOffer, highRateOffer));

        List<LoanOfferDto> result = calculateLoanTermsServiceImpl.calculateLoanTerms(request);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("18.0"), result.get(0).getRate());
        assertEquals(statementId, result.get(0).getStatementId());
        assertEquals(statementId, result.get(1).getStatementId());

        org.mockito.ArgumentCaptor<Statement> statementCaptor =
                org.mockito.ArgumentCaptor.forClass(Statement.class);
        verify(statementRepository).save(statementCaptor.capture());
        Statement persistedStatement = statementCaptor.getValue();
        assertEquals(ApplicationStatus.PREAPPROVAL, persistedStatement.getStatus());
        assertNotNull(persistedStatement.getStatusHistory());
        assertEquals(1, persistedStatement.getStatusHistory().size());
    }

    @Test
    void shouldThrowWhenOffersAreEmpty() {
        LoanStatementRequestDto request = loanStatementRequest();
        Client builtClient = client();
        Statement savedStatement = Statement.builder()
                .id(UUID.randomUUID())
                .client(builtClient)
                .build();

        when(clientBuilderMock.buildClient(request)).thenReturn(builtClient);
        when(clientRepository.save(builtClient)).thenReturn(builtClient);
        when(statementAndStatusHistoryBuilderMock.buildHistoryItem(any(), any())).thenCallRealMethod();
        when(statementRepository.save(any(Statement.class))).thenReturn(savedStatement);
        when(calculatorClient.getOffers(request)).thenReturn(List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> calculateLoanTermsServiceImpl.calculateLoanTerms(request)
        );

        assertEquals("Не удалось получить предложения по кредиту", exception.getMessage());
    }

    @Test
    void shouldFinishRegistrationCalculateCreditAndUpdateStatement() {
        UUID statementId = UUID.randomUUID();
        FinishRegistrationRequestDto request = finishRegistrationRequest();
        Statement statement = statement(statementId);
        Client client = statement.getClient();
        statement.setAppliedOffer(loanOffer(statementId, "15.0"));

        ScoringDataDto scoringDataDto = new ScoringDataDto(
                statement.getAppliedOffer().getRequestedAmount(),
                statement.getAppliedOffer().getTerm(),
                client.getFirstName(),
                client.getLastName(),
                client.getMiddleName(),
                request.getGender(),
                client.getBirthDate(),
                client.getPassport().getSeries(),
                client.getPassport().getNumber(),
                request.getPassportIssueDate(),
                request.getPassportIssueBranch(),
                request.getMaritalStatus(),
                request.getDependentAmount(),
                request.getEmployment(),
                request.getAccountNumber(),
                statement.getAppliedOffer().getIsInsuranceEnabled(),
                statement.getAppliedOffer().getIsSalaryClient()
        );

        CreditDto creditDto = creditDto();
        Credit builtCredit = Credit.builder().amount(creditDto.getAmount()).build();
        Credit savedCredit = Credit.builder()
                .id(UUID.randomUUID())
                .amount(creditDto.getAmount())
                .build();

        when(statementAndStatusHistoryBuilderMock.getStatementById(statementId)).thenReturn(statement);
        when(scoringDataBuilderMock.buildScoringData(statement, request)).thenReturn(scoringDataDto);
        when(calculatorClient.calc(scoringDataDto)).thenReturn(creditDto);
        when(creditBuilderMock.buildCredit(creditDto)).thenReturn(builtCredit);
        when(creditRepository.save(builtCredit)).thenReturn(savedCredit);
        when(statementAndStatusHistoryBuilderMock.buildHistoryItem(any(), any())).thenCallRealMethod();

        doAnswer(invocation -> {
            Statement st = invocation.getArgument(0);
            if (st.getStatusHistory() == null) {
                st.setStatusHistory(new ArrayList<>());
            }
            st.getStatusHistory().add(
                    statementAndStatusHistoryBuilderMock.buildHistoryItem(
                            invocation.getArgument(1),
                            invocation.getArgument(2)
                    )
            );
            return null;
        }).when(statementAndStatusHistoryBuilderMock).appendStatusHistory(any(Statement.class), any(), any());

        finishRegistrationAndCalculateServiceImpl.finishRegistrationAndCalculate(statementId, request);

        verify(clientBuilderMock).enrichClient(client, request);
        verify(clientRepository).save(client);
        verify(statementRepository).save(statement);
        assertEquals(savedCredit, statement.getCredit());
        assertEquals(ApplicationStatus.CC_APPROVED, statement.getStatus());
        assertNotNull(statement.getStatusHistory());
        assertEquals(ApplicationStatus.CC_APPROVED, statement.getStatusHistory().getLast().getStatus());
    }

    @Test
    void shouldUpdateStatementWhenOfferIsSelected() {
        UUID statementId = UUID.randomUUID();
        LoanOfferDto offerDto = loanOffer(statementId, "13.0");
        Statement statement = statement(statementId);

        when(statementAndStatusHistoryBuilderMock.getStatementById(statementId)).thenReturn(statement);

        doAnswer(invocation -> {
            Statement st = invocation.getArgument(0);
            st.setStatusHistory(new ArrayList<>());
            st.getStatusHistory().add(
                    statementAndStatusHistoryBuilderMock.buildHistoryItem(
                            invocation.getArgument(1),
                            invocation.getArgument(2)
                    )
            );
            return null;
        }).when(statementAndStatusHistoryBuilderMock).appendStatusHistory(
                org.mockito.ArgumentMatchers.same(statement),
                org.mockito.ArgumentMatchers.eq(ApplicationStatus.APPROVED),
                org.mockito.ArgumentMatchers.eq(ChangeType.MANUAL)
        );

        when(statementAndStatusHistoryBuilderMock.buildHistoryItem(
                ApplicationStatus.APPROVED,
                ChangeType.MANUAL
        )).thenCallRealMethod();

        selectOfferServiceImpl.selectOffer(offerDto);

        assertEquals(offerDto, statement.getAppliedOffer());
        assertEquals(ApplicationStatus.APPROVED, statement.getStatus());
        assertNotNull(statement.getStatusHistory());
        verify(statementRepository).save(statement);
    }

    @Test
    void shouldThrowWhenStatementIdIsMissing() {
        LoanOfferDto offerDto = loanOffer(null, "13.0");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> selectOfferServiceImpl.selectOffer(offerDto)
        );

        assertEquals("В предложении отсутствует statementId", exception.getMessage());
        verify(statementRepository, never()).save(any());
    }

    private LoanStatementRequestDto loanStatementRequest() {
        return new LoanStatementRequestDto(
                new BigDecimal("300000"),
                12,
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "ivanov@example.com",
                LocalDate.of(1990, 1, 10),
                "1234",
                "567890"
        );
    }

    private EmploymentDto employmentDto() {
        return new EmploymentDto(
                EmploymentStatus.EMPLOYED,
                "123456789012",
                new BigDecimal("150000"),
                Position.WORKER,
                36,
                12
        );
    }

    private FinishRegistrationRequestDto finishRegistrationRequest() {
        return new FinishRegistrationRequestDto(
                Gender.MALE,
                MaritalStatus.MARRIED,
                2,
                LocalDate.of(2015, 5, 20),
                "UFMS-770",
                employmentDto(),
                "40856820000000000001"
        );
    }

    private LoanOfferDto loanOffer(UUID statementId, String rate) {
        return new LoanOfferDto(
                statementId,
                new BigDecimal("300000"),
                new BigDecimal("330000"),
                12,
                new BigDecimal("27500"),
                new BigDecimal(rate),
                true,
                false
        );
    }

    private CreditDto creditDto() {
        return new CreditDto(
                new BigDecimal("300000"),
                12,
                new BigDecimal("27500"),
                new BigDecimal("15.5"),
                new BigDecimal("16.1"),
                true,
                false,
                List.of(new PaymentScheduleElementDto(
                        1,
                        LocalDate.of(2026, 1, 10),
                        new BigDecimal("27500"),
                        new BigDecimal("3000"),
                        new BigDecimal("24500"),
                        new BigDecimal("275500")
                ))
        );
    }

    private Client client() {
        LoanStatementRequestDto request = loanStatementRequest();
        return Client.builder()
                .id(UUID.randomUUID())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .email(request.getEmail())
                .birthDate(request.getBirthdate())
                .passport(Passport.builder()
                        .series(request.getPassportSeries())
                        .number(request.getPassportNumber())
                        .build())
                .build();
    }

    private Statement statement(UUID statementId) {
        return Statement.builder()
                .id(statementId)
                .client(client())
                .status(ApplicationStatus.PREAPPROVAL)
                .creationDate(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();
    }
}
