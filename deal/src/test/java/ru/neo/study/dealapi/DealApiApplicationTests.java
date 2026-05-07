package ru.neo.study.dealapi;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import ru.neo.study.dealapi.calculatorClient.CalculatorClient;
import ru.neo.study.dealapi.controller.DealApiController;
import ru.neo.study.dealapi.dto.CreditDto;
import ru.neo.study.dealapi.dto.EmploymentDto;
import ru.neo.study.dealapi.dto.ErrorResponseDto;
import ru.neo.study.dealapi.dto.FinishRegistrationRequestDto;
import ru.neo.study.dealapi.dto.LoanOfferDto;
import ru.neo.study.dealapi.dto.LoanStatementRequestDto;
import ru.neo.study.dealapi.dto.PaymentScheduleElementDto;
import ru.neo.study.dealapi.dto.ScoringDataDto;
import ru.neo.study.dealapi.dto.StatementStatusHistoryDto;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Credit;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.enums.ApplicationStatus;
import ru.neo.study.dealapi.enums.ChangeType;
import ru.neo.study.dealapi.enums.CreditStatus;
import ru.neo.study.dealapi.enums.EmploymentStatus;
import ru.neo.study.dealapi.enums.Gender;
import ru.neo.study.dealapi.enums.MaritalStatus;
import ru.neo.study.dealapi.enums.Position;
import ru.neo.study.dealapi.exception.ErrorHandler;
import ru.neo.study.dealapi.jsonb.Employment;
import ru.neo.study.dealapi.jsonb.Passport;
import ru.neo.study.dealapi.mapper.ClientMapper;
import ru.neo.study.dealapi.mapper.CreditMapper;
import ru.neo.study.dealapi.mapper.ScoringDataMapper;
import ru.neo.study.dealapi.mapper.StatementMapper;
import ru.neo.study.dealapi.repository.ClientRepository;
import ru.neo.study.dealapi.repository.CreditRepository;
import ru.neo.study.dealapi.repository.StatementRepository;
import ru.neo.study.dealapi.service.DealApiService;
import ru.neo.study.dealapi.service.DealApiServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DealApiApplicationTests {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ControllerTests {

        @Mock
        private DealApiService dealApiService;

        @InjectMocks
        private DealApiController controller;

        @Test
        void calculateLoanTermsShouldReturnCreatedAndOffers() {
            LoanStatementRequestDto request = loanStatementRequest();
            List<LoanOfferDto> offers = List.of(loanOffer(UUID.randomUUID(), "15.90"));

            when(dealApiService.calculateLoanTerms(request)).thenReturn(offers);

            ResponseEntity<List<LoanOfferDto>> response = controller.calculateLoanTerms(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(offers);

            verify(dealApiService).calculateLoanTerms(request);
        }

        @Test
        void selectOfferShouldReturnOkAndLocationHeader() {
            UUID statementId = UUID.randomUUID();
            LoanOfferDto offer = loanOffer(statementId, "14.10");

            ResponseEntity<Void> response = controller.selectOffer(offer);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst("Location"))
                    .isEqualTo("/statement/" + statementId);

            verify(dealApiService).selectOffer(offer);
        }

        @Test
        void finishRegistrationAndCalculateShouldReturnOkAndStatementStatusHeader() {
            UUID statementId = UUID.randomUUID();
            FinishRegistrationRequestDto request = finishRegistrationRequest();

            when(dealApiService.getStatementStatus(statementId)).thenReturn("CC_APPROVED");

            ResponseEntity<Void> response = controller.finishRegistrationAndCalculate(statementId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst("StatementStatus")).isEqualTo("CC_APPROVED");

            verify(dealApiService).finishRegistrationAndCalculate(statementId, request);
            verify(dealApiService).getStatementStatus(statementId);
        }
    }

    @Nested
    class ErrorHandlerTests {

        private final ru.neo.study.dealapi.exception.ErrorHandler errorHandler = new ErrorHandler();

        @Test
        void handleOptimisticLockingFailureExceptionShouldReturnConflictResponse() {
            UUID statementId = UUID.randomUUID();
            ObjectOptimisticLockingFailureException exception =
                    new ObjectOptimisticLockingFailureException(Statement.class, statementId);

            ResponseEntity<ErrorResponseDto> response =
                    errorHandler.handleOptimisticLockingFailureException(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            ErrorResponseDto body = response.getBody();

            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(409);
            assertThat(body.getError()).isEqualTo("Конфликт при обновлении заявки");
            assertThat(body.getDescription())
                    .isEqualTo("Заявка была изменена другим запросом. Обновите данные и повторите попытку.");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ServiceTests {

        @Mock
        private ClientRepository clientRepository;

        @Mock
        private StatementRepository statementRepository;

        @Mock
        private CreditRepository creditRepository;

        @Mock
        private CalculatorClient calculatorClient;

        private final ClientMapper clientMapper = Mappers.getMapper(ClientMapper.class);
        private final StatementMapper statementMapper = Mappers.getMapper(StatementMapper.class);
        private final ScoringDataMapper scoringDataMapper = Mappers.getMapper(ScoringDataMapper.class);
        private final CreditMapper creditMapper = Mappers.getMapper(CreditMapper.class);

        private DealApiServiceImpl service;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            service = new DealApiServiceImpl(
                    clientRepository,
                    statementRepository,
                    calculatorClient,
                    clientMapper,
                    statementMapper,
                    scoringDataMapper,
                    creditRepository,
                    creditMapper
            );
        }

        @Test
        void calculateLoanTermsShouldCreateClientAndStatementAndReturnSortedOffers() {
            LoanStatementRequestDto request = loanStatementRequest();

            Client savedClient = new Client(
                    UUID.randomUUID(),
                    request.getLastName(),
                    request.getFirstName(),
                    request.getMiddleName(),
                    request.getBirthdate(),
                    request.getEmail(),
                    null,
                    null,
                    null,
                    new Passport(UUID.randomUUID(), request.getPassportSeries(), request.getPassportNumber(), null, null),
                    null,
                    null
            );

            Statement savedStatement = Statement.builder()
                    .id(UUID.randomUUID())
                    .client(savedClient)
                    .status(ApplicationStatus.PREAPPROVAL)
                    .creationDate(LocalDateTime.now())
                    .statusHistory(List.of(
                            new StatementStatusHistoryDto(
                                    ApplicationStatus.PREAPPROVAL,
                                    LocalDateTime.now(),
                                    ChangeType.AUTOMATIC
                            )
                    ))
                    .build();

            LoanOfferDto lowRateOffer = new LoanOfferDto(
                    null,
                    new BigDecimal("300000"),
                    new BigDecimal("334000"),
                    24,
                    new BigDecimal("15200"),
                    new BigDecimal("13.50"),
                    true,
                    false
            );

            LoanOfferDto highRateOffer = new LoanOfferDto(
                    null,
                    new BigDecimal("300000"),
                    new BigDecimal("348000"),
                    24,
                    new BigDecimal("15700"),
                    new BigDecimal("16.20"),
                    false,
                    true
            );

            when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
            when(statementRepository.save(any(Statement.class))).thenReturn(savedStatement);
            when(calculatorClient.getOffers(request)).thenReturn(List.of(lowRateOffer, highRateOffer));

            List<LoanOfferDto> result = service.calculateLoanTerms(request);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRate()).isEqualByComparingTo("16.20");
            assertThat(result.get(1).getRate()).isEqualByComparingTo("13.50");
            assertThat(result).allMatch(offer -> savedStatement.getId().equals(offer.getStatementId()));

            verify(clientRepository).save(any(Client.class));
            verify(statementRepository).save(any(Statement.class));
            verify(calculatorClient).getOffers(request);
        }

        @Test
        void calculateLoanTermsShouldThrowWhenCalculatorReturnsNull() {
            LoanStatementRequestDto request = loanStatementRequest();

            Client savedClient = new Client();
            savedClient.setId(UUID.randomUUID());

            Statement savedStatement = Statement.builder()
                    .id(UUID.randomUUID())
                    .client(savedClient)
                    .build();

            when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
            when(statementRepository.save(any(Statement.class))).thenReturn(savedStatement);
            when(calculatorClient.getOffers(request)).thenReturn(null);

            assertThatThrownBy(() -> service.calculateLoanTerms(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Не удалось получить предложения по кредиту");
        }

        @Test
        void calculateLoanTermsShouldThrowWhenCalculatorReturnsEmptyList() {
            LoanStatementRequestDto request = loanStatementRequest();

            Client savedClient = new Client();
            savedClient.setId(UUID.randomUUID());

            Statement savedStatement = Statement.builder()
                    .id(UUID.randomUUID())
                    .client(savedClient)
                    .build();

            when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
            when(statementRepository.save(any(Statement.class))).thenReturn(savedStatement);
            when(calculatorClient.getOffers(request)).thenReturn(List.of());

            assertThatThrownBy(() -> service.calculateLoanTerms(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Не удалось получить предложения по кредиту");
        }

        @Test
        void selectOfferShouldSaveAppliedOfferAndApproveStatement() {
            UUID statementId = UUID.randomUUID();
            LoanOfferDto offer = loanOffer(statementId, "14.40");

            Statement statement = baseStatement(statementId);
            statement.setStatus(ApplicationStatus.PREAPPROVAL);
            statement.setStatusHistory(List.of(
                    new StatementStatusHistoryDto(
                            ApplicationStatus.PREAPPROVAL,
                            LocalDateTime.now().minusMinutes(10),
                            ChangeType.AUTOMATIC
                    )
            ));

            when(statementRepository.findByIdWithBlock(statementId)).thenReturn(Optional.of(statement));

            service.selectOffer(offer);

            assertThat(statement.getAppliedOffer()).isEqualTo(offer);
            assertThat(statement.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(statement.getStatusHistory()).hasSize(2);
            assertThat(statement.getStatusHistory().get(1).getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(statement.getStatusHistory().get(1).getChangeType()).isEqualTo(ChangeType.MANUAL);

            verify(statementRepository).findByIdWithBlock(statementId);
            verify(statementRepository).save(statement);
        }

        @Test
        void selectOfferShouldPropagateOptimisticLockingFailureException() {
            UUID statementId = UUID.randomUUID();
            LoanOfferDto offer = loanOffer(statementId, "14.40");

            Statement statement = baseStatement(statementId);
            when(statementRepository.findByIdWithBlock(statementId)).thenReturn(Optional.of(statement));
            when(statementRepository.save(statement))
                    .thenThrow(new ObjectOptimisticLockingFailureException(Statement.class, statementId));

            assertThatThrownBy(() -> service.selectOffer(offer))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);

            verify(statementRepository).findByIdWithBlock(statementId);
            verify(statementRepository).save(statement);
        }

        @Test
        void selectOfferShouldThrowWhenStatementIdIsMissing() {
            LoanOfferDto offer = loanOffer(null, "14.40");

            assertThatThrownBy(() -> service.selectOffer(offer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("В предложении отсутствует statementId");

            verifyNoInteractions(statementRepository);
        }

        @Test
        void selectOfferShouldThrowWhenStatementNotFound() {
            UUID statementId = UUID.randomUUID();
            LoanOfferDto offer = loanOffer(statementId, "14.40");

            when(statementRepository.findByIdWithBlock(statementId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.selectOffer(offer))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(statementId.toString());
        }

        @Test
        void finishRegistrationAndCalculateShouldUpdateClientSaveCreditAndApproveStatement() {
            UUID statementId = UUID.randomUUID();

            FinishRegistrationRequestDto request = finishRegistrationRequest();
            Statement statement = baseStatement(statementId);
            statement.setStatus(ApplicationStatus.APPROVED);
            statement.setAppliedOffer(loanOffer(statementId, "12.90"));
            statement.setStatusHistory(List.of(
                    new StatementStatusHistoryDto(
                            ApplicationStatus.PREAPPROVAL,
                            LocalDateTime.now().minusHours(1),
                            ChangeType.AUTOMATIC
                    ),
                    new StatementStatusHistoryDto(
                            ApplicationStatus.APPROVED,
                            LocalDateTime.now().minusMinutes(30),
                            ChangeType.MANUAL
                    )
            ));

            CreditDto creditDto = creditDto();
            Credit savedCredit = Credit.builder()
                    .id(UUID.randomUUID())
                    .amount(creditDto.getAmount())
                    .term(creditDto.getTerm())
                    .monthlyPayment(creditDto.getMonthlyPayment())
                    .rate(creditDto.getRate())
                    .psk(creditDto.getPsk())
                    .paymentSchedule(creditDto.getPaymentSchedule())
                    .insuranceEnabled(creditDto.getIsInsuranceEnabled())
                    .salaryClient(creditDto.getIsSalaryClient())
                    .creditStatus(CreditStatus.CALCULATED)
                    .build();

            when(statementRepository.findById(statementId)).thenReturn(Optional.of(statement));
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(calculatorClient.calc(any(ScoringDataDto.class))).thenReturn(creditDto);
            when(creditRepository.save(any(Credit.class))).thenReturn(savedCredit);

            service.finishRegistrationAndCalculate(statementId, request);

            assertThat(statement.getClient().getGender()).isEqualTo(request.getGender());
            assertThat(statement.getClient().getMaritalStatus()).isEqualTo(request.getMaritalStatus());
            assertThat(statement.getClient().getDependentAmount()).isEqualTo(request.getDependentAmount());
            assertThat(statement.getClient().getAccountNumber()).isEqualTo(request.getAccountNumber());

            assertThat(statement.getClient().getPassport()).isNotNull();
            assertThat(statement.getClient().getPassport().getIssueDate()).isEqualTo(request.getPassportIssueDate());
            assertThat(statement.getClient().getPassport().getIssueBranch()).isEqualTo(request.getPassportIssueBranch());

            assertThat(statement.getClient().getEmployment()).isNotNull();
            assertThat(statement.getClient().getEmployment().getStatus())
                    .isEqualTo(request.getEmployment().getEmploymentStatus());
            assertThat(statement.getClient().getEmployment().getEmployerInn())
                    .isEqualTo(request.getEmployment().getEmployerINN());

            assertThat(statement.getCredit()).isEqualTo(savedCredit);
            assertThat(statement.getStatus()).isEqualTo(ApplicationStatus.CC_APPROVED);
            assertThat(statement.getStatusHistory()).hasSize(3);
            assertThat(statement.getStatusHistory().get(2).getStatus()).isEqualTo(ApplicationStatus.CC_APPROVED);
            assertThat(statement.getStatusHistory().get(2).getChangeType()).isEqualTo(ChangeType.AUTOMATIC);

            verify(statementRepository).findById(statementId);
            verify(clientRepository).save(statement.getClient());
            verify(calculatorClient).calc(any(ScoringDataDto.class));
            verify(creditRepository).save(any(Credit.class));

            ArgumentCaptor<Statement> statementCaptor = ArgumentCaptor.forClass(Statement.class);
            verify(statementRepository).save(statementCaptor.capture());

            assertThat(statementCaptor.getValue().getStatus()).isEqualTo(ApplicationStatus.CC_APPROVED);
            assertThat(statementCaptor.getValue().getCredit()).isEqualTo(savedCredit);
        }

        @Test
        void finishRegistrationAndCalculateShouldThrowWhenStatementNotFound() {
            UUID statementId = UUID.randomUUID();

            when(statementRepository.findById(statementId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.finishRegistrationAndCalculate(statementId, finishRegistrationRequest()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(statementId.toString());
        }

        @Test
        void getStatementStatusShouldReturnStatusName() {
            UUID statementId = UUID.randomUUID();
            Statement statement = baseStatement(statementId);
            statement.setStatus(ApplicationStatus.CC_APPROVED);

            when(statementRepository.findById(statementId)).thenReturn(Optional.of(statement));

            String status = service.getStatementStatus(statementId);

            assertThat(status).isEqualTo("CC_APPROVED");
        }

        @Test
        void getStatementStatusShouldThrowWhenStatementNotFound() {
            UUID statementId = UUID.randomUUID();

            when(statementRepository.findById(statementId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStatementStatus(statementId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(statementId.toString());
        }
    }

    @Nested
    class MapperTests {

        private final ClientMapper clientMapper = Mappers.getMapper(ClientMapper.class);
        private final CreditMapper creditMapper = Mappers.getMapper(CreditMapper.class);
        private final ScoringDataMapper scoringDataMapper = Mappers.getMapper(ScoringDataMapper.class);
        private final StatementMapper statementMapper = Mappers.getMapper(StatementMapper.class);

        @Test
        void clientMapperShouldMapLoanStatementRequestToClient() {
            LoanStatementRequestDto request = loanStatementRequest();

            Client client = clientMapper.toEntity(request);

            assertThat(client.getId()).isNull();
            assertThat(client.getFirstName()).isEqualTo(request.getFirstName());
            assertThat(client.getLastName()).isEqualTo(request.getLastName());
            assertThat(client.getMiddleName()).isEqualTo(request.getMiddleName());
            assertThat(client.getBirthDate()).isEqualTo(request.getBirthdate());
            assertThat(client.getEmail()).isEqualTo(request.getEmail());
            assertThat(client.getPassport()).isNotNull();
            assertThat(client.getPassport().getId()).isNotNull();
            assertThat(client.getPassport().getSeries()).isEqualTo(request.getPassportSeries());
            assertThat(client.getPassport().getNumber()).isEqualTo(request.getPassportNumber());
            assertThat(client.getEmployment()).isNull();
            assertThat(client.getGender()).isNull();
            assertThat(client.getMaritalStatus()).isNull();
            assertThat(client.getDependentAmount()).isNull();
            assertThat(client.getAccountNumber()).isNull();
        }

        @Test
        void clientMapperShouldUpdateClientFromFinishRegistration() {
            Client client = clientMapper.toEntity(loanStatementRequest());
            FinishRegistrationRequestDto request = finishRegistrationRequest();

            clientMapper.updateClientFromFinishRegistration(request, client);

            assertThat(client.getGender()).isEqualTo(request.getGender());
            assertThat(client.getMaritalStatus()).isEqualTo(request.getMaritalStatus());
            assertThat(client.getDependentAmount()).isEqualTo(request.getDependentAmount());
            assertThat(client.getAccountNumber()).isEqualTo(request.getAccountNumber());

            assertThat(client.getEmployment()).isNotNull();
            assertThat(client.getEmployment().getId()).isNotNull();
            assertThat(client.getEmployment().getStatus()).isEqualTo(request.getEmployment().getEmploymentStatus());
            assertThat(client.getEmployment().getEmployerInn()).isEqualTo(request.getEmployment().getEmployerINN());
            assertThat(client.getEmployment().getSalary()).isEqualByComparingTo(request.getEmployment().getSalary());
            assertThat(client.getEmployment().getPosition()).isEqualTo(request.getEmployment().getPosition());

            assertThat(client.getPassport()).isNotNull();
            assertThat(client.getPassport().getId()).isNotNull();
            assertThat(client.getPassport().getIssueDate()).isEqualTo(request.getPassportIssueDate());
            assertThat(client.getPassport().getIssueBranch()).isEqualTo(request.getPassportIssueBranch());
        }

        @Test
        void clientMapperShouldCreatePassportIfMissingOnUpdate() {
            Client client = new Client();
            client.setPassport(null);

            clientMapper.updateClientFromFinishRegistration(finishRegistrationRequest(), client);

            assertThat(client.getPassport()).isNotNull();
            assertThat(client.getPassport().getId()).isNotNull();
            assertThat(client.getPassport().getIssueDate()).isEqualTo(finishRegistrationRequest().getPassportIssueDate());
            assertThat(client.getPassport().getIssueBranch()).isEqualTo(finishRegistrationRequest().getPassportIssueBranch());
        }

        @Test
        void creditMapperShouldMapCreditDtoToCredit() {
            CreditDto dto = creditDto();

            Credit credit = creditMapper.toEntity(dto);

            assertThat(credit.getId()).isNull();
            assertThat(credit.getAmount()).isEqualByComparingTo(dto.getAmount());
            assertThat(credit.getTerm()).isEqualTo(dto.getTerm());
            assertThat(credit.getMonthlyPayment()).isEqualByComparingTo(dto.getMonthlyPayment());
            assertThat(credit.getRate()).isEqualByComparingTo(dto.getRate());
            assertThat(credit.getPsk()).isEqualByComparingTo(dto.getPsk());
            assertThat(credit.getInsuranceEnabled()).isEqualTo(dto.getIsInsuranceEnabled());
            assertThat(credit.getSalaryClient()).isEqualTo(dto.getIsSalaryClient());
            assertThat(credit.getPaymentSchedule()).isEqualTo(dto.getPaymentSchedule());
            assertThat(credit.getCreditStatus()).isEqualTo(CreditStatus.CALCULATED);
        }

        @Test
        void scoringDataMapperShouldMapClientStatementAndRequestToDto() {
            Client client = baseClient();
            Statement statement = Statement.builder()
                    .id(UUID.randomUUID())
                    .client(client)
                    .appliedOffer(loanOffer(UUID.randomUUID(), "13.80"))
                    .build();
            FinishRegistrationRequestDto request = finishRegistrationRequest();

            ScoringDataDto dto = scoringDataMapper.toDto(client, statement, request);

            assertThat(dto.getAmount()).isEqualByComparingTo(statement.getAppliedOffer().getRequestedAmount());
            assertThat(dto.getTerm()).isEqualTo(statement.getAppliedOffer().getTerm());
            assertThat(dto.getFirstName()).isEqualTo(client.getFirstName());
            assertThat(dto.getLastName()).isEqualTo(client.getLastName());
            assertThat(dto.getMiddleName()).isEqualTo(client.getMiddleName());
            assertThat(dto.getGender()).isEqualTo(request.getGender());
            assertThat(dto.getBirthdate()).isEqualTo(client.getBirthDate());
            assertThat(dto.getPassportSeries()).isEqualTo(client.getPassport().getSeries());
            assertThat(dto.getPassportNumber()).isEqualTo(client.getPassport().getNumber());
            assertThat(dto.getPassportIssueDate()).isEqualTo(request.getPassportIssueDate());
            assertThat(dto.getPassportIssueBranch()).isEqualTo(request.getPassportIssueBranch());
            assertThat(dto.getMaritalStatus()).isEqualTo(request.getMaritalStatus());
            assertThat(dto.getDependentAmount()).isEqualTo(request.getDependentAmount());
            assertThat(dto.getEmployment()).isEqualTo(request.getEmployment());
            assertThat(dto.getAccountNumber()).isEqualTo(request.getAccountNumber());
            assertThat(dto.getIsInsuranceEnabled()).isEqualTo(statement.getAppliedOffer().getIsInsuranceEnabled());
            assertThat(dto.getIsSalaryClient()).isEqualTo(statement.getAppliedOffer().getIsSalaryClient());
        }

        @Test
        void statementMapperShouldCreateNewStatementWithInitialHistory() {
            Client client = baseClient();

            Statement statement = statementMapper.toNewStatement(
                    client,
                    ApplicationStatus.PREAPPROVAL,
                    ChangeType.AUTOMATIC
            );

            assertThat(statement.getId()).isNull();
            assertThat(statement.getClient()).isEqualTo(client);
            assertThat(statement.getCredit()).isNull();
            assertThat(statement.getStatus()).isEqualTo(ApplicationStatus.PREAPPROVAL);
            assertThat(statement.getCreationDate()).isNotNull();
            assertThat(statement.getAppliedOffer()).isNull();
            assertThat(statement.getSignDate()).isNull();
            assertThat(statement.getSesCode()).isNull();
            assertThat(statement.getStatusHistory()).hasSize(1);
            assertThat(statement.getStatusHistory().get(0).getStatus()).isEqualTo(ApplicationStatus.PREAPPROVAL);
            assertThat(statement.getStatusHistory().get(0).getChangeType()).isEqualTo(ChangeType.AUTOMATIC);
            assertThat(statement.getStatusHistory().get(0).getTime()).isNotNull();
        }

        @Test
        void statementMapperShouldAppendStatusHistoryWhenHistoryIsNull() {
            Statement statement = Statement.builder()
                    .statusHistory(null)
                    .build();

            statementMapper.appendStatusHistory(statement, ApplicationStatus.APPROVED, ChangeType.MANUAL);

            assertThat(statement.getStatusHistory()).hasSize(1);
            assertThat(statement.getStatusHistory().get(0).getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(statement.getStatusHistory().get(0).getChangeType()).isEqualTo(ChangeType.MANUAL);
        }

        @Test
        void statementMapperShouldAppendStatusHistoryWhenHistoryAlreadyExists() {
            Statement statement = Statement.builder()
                    .statusHistory(List.of(
                            new StatementStatusHistoryDto(
                                    ApplicationStatus.PREAPPROVAL,
                                    LocalDateTime.now().minusHours(1),
                                    ChangeType.AUTOMATIC
                            )
                    ))
                    .build();

            statementMapper.appendStatusHistory(statement, ApplicationStatus.CC_APPROVED, ChangeType.AUTOMATIC);

            assertThat(statement.getStatusHistory()).hasSize(2);
            assertThat(statement.getStatusHistory().get(1).getStatus()).isEqualTo(ApplicationStatus.CC_APPROVED);
            assertThat(statement.getStatusHistory().get(1).getChangeType()).isEqualTo(ChangeType.AUTOMATIC);
        }
    }

    private static LoanStatementRequestDto loanStatementRequest() {
        return new LoanStatementRequestDto(
                new BigDecimal("300000"),
                24,
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "ivanov@test.ru",
                LocalDate.of(1995, 5, 20),
                "1234",
                "567890"
        );
    }

    private static FinishRegistrationRequestDto finishRegistrationRequest() {
        return new FinishRegistrationRequestDto(
                Gender.MALE,
                MaritalStatus.MARRIED,
                2,
                LocalDate.of(2015, 4, 12),
                "ОВД 770-001",
                new EmploymentDto(
                        EmploymentStatus.EMPLOYED,
                        "7701234567",
                        new BigDecimal("150000"),
                        Position.MID_MANAGEMENT,
                        120,
                        36
                ),
                "40817810099910004312"
        );
    }

    private static CreditDto creditDto() {
        return new CreditDto(
                new BigDecimal("300000"),
                24,
                new BigDecimal("14500.00"),
                new BigDecimal("12.90"),
                new BigDecimal("13.70"),
                true,
                false,
                List.of(
                        new PaymentScheduleElementDto(
                                1,
                                LocalDate.of(2026, 5, 15),
                                new BigDecimal("14500.00"),
                                new BigDecimal("3200.00"),
                                new BigDecimal("11300.00"),
                                new BigDecimal("288700.00")
                        )
                )
        );
    }

    static LoanOfferDto loanOffer(UUID statementId, String rate) {
        return new LoanOfferDto(
                statementId,
                new BigDecimal("300000"),
                new BigDecimal("348000"),
                24,
                new BigDecimal("14500.00"),
                new BigDecimal(rate),
                true,
                false
        );
    }

    private static Client baseClient() {
        return new Client(
                UUID.randomUUID(),
                "Ivanov",
                "Ivan",
                "Ivanovich",
                LocalDate.of(1995, 5, 20),
                "ivanov@test.ru",
                Gender.MALE,
                MaritalStatus.MARRIED,
                1,
                new Passport(
                        UUID.randomUUID(),
                        "1234",
                        "567890",
                        null,
                        null
                ),
                new Employment(
                        UUID.randomUUID(),
                        EmploymentStatus.EMPLOYED,
                        "7701234567",
                        new BigDecimal("150000"),
                        Position.MID_MANAGEMENT,
                        120,
                        36
                ),
                "40817810099910004312"
        );
    }

    private static Statement baseStatement(UUID statementId) {
        return Statement.builder()
                .id(statementId)
                .client(baseClient())
                .status(ApplicationStatus.PREAPPROVAL)
                .creationDate(LocalDateTime.now())
                .statusHistory(List.of())
                .build();
    }
}