package ru.neo.study.dealapi.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import ru.neo.study.dealapi.calculatorClient.CalculatorClient;
import ru.neo.study.dealapi.dto.CreditDto;
import ru.neo.study.dealapi.dto.FinishRegistrationRequestDto;
import ru.neo.study.dealapi.dto.LoanOfferDto;
import ru.neo.study.dealapi.dto.LoanStatementRequestDto;
import ru.neo.study.dealapi.dto.ScoringDataDto;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Credit;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.enums.ApplicationStatus;
import ru.neo.study.dealapi.enums.ChangeType;
import ru.neo.study.dealapi.mapper.ClientMapper;
import ru.neo.study.dealapi.mapper.CreditMapper;
import ru.neo.study.dealapi.mapper.ScoringDataMapper;
import ru.neo.study.dealapi.mapper.StatementMapper;
import ru.neo.study.dealapi.repository.ClientRepository;
import ru.neo.study.dealapi.repository.CreditRepository;
import ru.neo.study.dealapi.repository.StatementRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealApiServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private StatementRepository statementRepository;

    @Mock
    private CreditRepository creditRepository;

    @Mock
    private CalculatorClient calculatorClient;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private StatementMapper statementMapper;

    @Mock
    private ScoringDataMapper scoringDataMapper;

    @Mock
    private CreditMapper creditMapper;

    @InjectMocks
    private DealApiServiceImpl service;

    @Test
    void calculateLoanTermsShouldSortOffersAndSetStatementId() {
        LoanStatementRequestDto request = mock(LoanStatementRequestDto.class);
        Client client = new Client();
        UUID statementId = UUID.randomUUID();

        Statement statement = Statement.builder()
                .id(statementId)
                .client(client)
                .build();

        LoanOfferDto lowRateOffer = mock(LoanOfferDto.class);
        LoanOfferDto highRateOffer = mock(LoanOfferDto.class);

        when(lowRateOffer.getRate()).thenReturn(new BigDecimal("10"));
        when(highRateOffer.getRate()).thenReturn(new BigDecimal("20"));

        when(clientMapper.toEntity(request)).thenReturn(client);
        when(clientRepository.save(client)).thenReturn(client);
        when(statementMapper.toNewStatement(client, ApplicationStatus.PREAPPROVAL, ChangeType.AUTOMATIC))
                .thenReturn(statement);
        when(statementRepository.save(statement)).thenReturn(statement);
        when(calculatorClient.getOffers(request)).thenReturn(List.of(lowRateOffer, highRateOffer));

        List<LoanOfferDto> result = service.calculateLoanTerms(request);

        assertThat(result).containsExactly(highRateOffer, lowRateOffer);

        verify(lowRateOffer).setStatementId(statementId);
        verify(highRateOffer).setStatementId(statementId);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void calculateLoanTermsShouldThrowWhenOffersAreMissing(List<LoanOfferDto> offers) {
        LoanStatementRequestDto request = mock(LoanStatementRequestDto.class);
        Client client = new Client();

        Statement statement = Statement.builder()
                .id(UUID.randomUUID())
                .client(client)
                .build();

        when(clientMapper.toEntity(request)).thenReturn(client);
        when(clientRepository.save(client)).thenReturn(client);
        when(statementMapper.toNewStatement(client, ApplicationStatus.PREAPPROVAL, ChangeType.AUTOMATIC))
                .thenReturn(statement);
        when(statementRepository.save(statement)).thenReturn(statement);
        when(calculatorClient.getOffers(request)).thenReturn(offers);

        assertThatThrownBy(() -> service.calculateLoanTerms(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Не удалось получить предложения по кредиту");
    }

    @Test
    void selectOfferShouldApproveStatement() {
        UUID statementId = UUID.randomUUID();
        LoanOfferDto offer = mock(LoanOfferDto.class);

        Statement statement = Statement.builder()
                .id(statementId)
                .status(ApplicationStatus.PREAPPROVAL)
                .build();

        when(offer.getStatementId()).thenReturn(statementId);
        when(statementRepository.findByIdWithBlock(statementId)).thenReturn(Optional.of(statement));

        service.selectOffer(offer);

        assertThat(statement.getAppliedOffer()).isSameAs(offer);
        assertThat(statement.getStatus()).isEqualTo(ApplicationStatus.APPROVED);

        verify(statementMapper).appendStatusHistory(statement, ApplicationStatus.APPROVED, ChangeType.MANUAL);
        verify(statementRepository).save(statement);
    }

    @Test
    void selectOfferShouldThrowWhenStatementIdIsNull() {
        LoanOfferDto offer = mock(LoanOfferDto.class);

        when(offer.getStatementId()).thenReturn(null);

        assertThatThrownBy(() -> service.selectOffer(offer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("В предложении отсутствует statementId");
    }

    @Test
    void selectOfferShouldThrowWhenStatementNotFound() {
        UUID statementId = UUID.randomUUID();
        LoanOfferDto offer = mock(LoanOfferDto.class);

        when(offer.getStatementId()).thenReturn(statementId);
        when(statementRepository.findByIdWithBlock(statementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.selectOffer(offer))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void finishRegistrationAndCalculateShouldApproveStatement() {
        UUID statementId = UUID.randomUUID();
        FinishRegistrationRequestDto request = mock(FinishRegistrationRequestDto.class);
        Client client = new Client();

        Statement statement = Statement.builder()
                .id(statementId)
                .client(client)
                .status(ApplicationStatus.APPROVED)
                .build();

        ScoringDataDto scoringData = mock(ScoringDataDto.class);
        CreditDto creditDto = mock(CreditDto.class);
        Credit credit = new Credit();

        when(statementRepository.findById(statementId)).thenReturn(Optional.of(statement));
        when(scoringDataMapper.toDto(client, statement, request)).thenReturn(scoringData);
        when(calculatorClient.calc(scoringData)).thenReturn(creditDto);
        when(creditMapper.toEntity(creditDto)).thenReturn(credit);
        when(creditRepository.save(credit)).thenReturn(credit);

        service.finishRegistrationAndCalculate(statementId, request);

        assertThat(statement.getCredit()).isSameAs(credit);
        assertThat(statement.getStatus()).isEqualTo(ApplicationStatus.CC_APPROVED);

        verify(clientMapper).updateClientFromFinishRegistration(request, client);
        verify(statementMapper).appendStatusHistory(statement, ApplicationStatus.CC_APPROVED, ChangeType.AUTOMATIC);
        verify(statementRepository).save(statement);
    }

    @Test
    void finishRegistrationAndCalculateShouldThrowWhenStatementNotFound() {
        UUID statementId = UUID.randomUUID();
        FinishRegistrationRequestDto request = mock(FinishRegistrationRequestDto.class);

        when(statementRepository.findById(statementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.finishRegistrationAndCalculate(statementId, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getStatementStatusShouldReturnStatusName() {
        UUID statementId = UUID.randomUUID();

        Statement statement = Statement.builder()
                .status(ApplicationStatus.CC_APPROVED)
                .build();

        when(statementRepository.findById(statementId)).thenReturn(Optional.of(statement));

        assertThat(service.getStatementStatus(statementId)).isEqualTo("CC_APPROVED");
    }

    @Test
    void getStatementStatusShouldThrowWhenStatementNotFound() {
        UUID statementId = UUID.randomUUID();

        when(statementRepository.findById(statementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatementStatus(statementId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void selectOfferShouldThrowOptimisticLockingExceptionWhenStatementWasChanged() {
        UUID statementId = UUID.randomUUID();

        LoanOfferDto offer = mock(LoanOfferDto.class);
        when(offer.getStatementId()).thenReturn(statementId);

        Statement statement = Statement.builder()
                .id(statementId)
                .status(ApplicationStatus.PREAPPROVAL)
                .build();

        when(statementRepository.findByIdWithBlock(statementId))
                .thenReturn(Optional.of(statement));

        when(statementRepository.save(statement))
                .thenThrow(new ObjectOptimisticLockingFailureException(Statement.class, statementId));

        assertThatThrownBy(() -> service.selectOffer(offer))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(statementRepository).findByIdWithBlock(statementId);
        verify(statementMapper).appendStatusHistory(
                statement,
                ApplicationStatus.APPROVED,
                ChangeType.MANUAL
        );
        verify(statementRepository).save(statement);
    }
}