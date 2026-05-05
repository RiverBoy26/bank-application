package ru.neo.study.statement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.neo.study.statement.controller.StatementController;
import ru.neo.study.statement.dealClient.DealClient;
import ru.neo.study.statement.dto.LoanOfferDto;
import ru.neo.study.statement.dto.LoanStatementRequestDto;
import ru.neo.study.statement.exception.ErrorHandler;
import ru.neo.study.statement.exception.OfferSelectionConflictException;
import ru.neo.study.statement.exception.OffersNotFoundException;
import ru.neo.study.statement.exception.ValidationException;
import ru.neo.study.statement.service.StatementService;
import ru.neo.study.statement.service.StatementServiceImpl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StatementApplicationTests {

    @Mock
    private StatementService statementService;

    @Mock
    private DealClient dealClient;

    private MockMvc mockMvc;
    private StatementServiceImpl service;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StatementController(statementService))
                .setControllerAdvice(new ErrorHandler())
                .build();

        service = new StatementServiceImpl(dealClient);
    }

    @Test
    void calculateLoanOffersShouldReturnCreatedAndOffers() throws Exception {
        LoanStatementRequestDto request = request();
        List<LoanOfferDto> offers = offers();

        when(statementService.calculateLoanOffers(any(LoanStatementRequestDto.class)))
                .thenReturn(offers);

        mockMvc.perform(post("/statement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(json(offers)));

        verify(statementService).calculateLoanOffers(any(LoanStatementRequestDto.class));
    }

    @Test
    void calculateLoanOffersShouldReturnNotFoundWhenOffersAreMissing() throws Exception {
        LoanStatementRequestDto request = request();

        when(statementService.calculateLoanOffers(any(LoanStatementRequestDto.class)))
                .thenThrow(new OffersNotFoundException("Кредитные предложения не найдены"));

        mockMvc.perform(post("/statement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {
                          "status": 404,
                          "error": "Кредитные предложения не найдены",
                          "description": "Кредитные предложения не найдены"
                        }
                        """));
    }

    @Test
    void selectOfferShouldReturnOk() throws Exception {
        LoanOfferDto offer = offer("15.50");

        mockMvc.perform(post("/statement/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(offer)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(statementService).selectOffer(any(LoanOfferDto.class));
    }

    @Test
    void selectOfferShouldReturnConflictWhenStatementWasChangedConcurrently() throws Exception {
        LoanOfferDto offer = offer("15.50");

        doThrow(new OfferSelectionConflictException(
                "Заявка была изменена другим запросом. Обновите данные и повторите попытку."
        )).when(statementService).selectOffer(any(LoanOfferDto.class));

        mockMvc.perform(post("/statement/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(offer)))
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {
                          "status": 409,
                          "error": "Конфликт при обновлении заявки",
                          "description": "Заявка была изменена другим запросом. Обновите данные и повторите попытку."
                        }
                        """));
    }

    @Test
    void serviceShouldReturnOffersFromDealClient() {
        LoanStatementRequestDto request = request();
        List<LoanOfferDto> offers = offers();

        when(dealClient.calculateLoanTerms(request))
                .thenReturn(ResponseEntity.ok(offers));

        assertThat(service.calculateLoanOffers(request)).isSameAs(offers);

        verify(dealClient).calculateLoanTerms(request);
    }

    @Test
    void serviceShouldRetryOnServerErrorAndReturnOffers() {
        LoanStatementRequestDto request = request();
        List<LoanOfferDto> offers = offers();

        when(dealClient.calculateLoanTerms(request))
                .thenThrow(feignException(500))
                .thenThrow(feignException(502))
                .thenReturn(ResponseEntity.ok(offers));

        assertThat(service.calculateLoanOffers(request)).isEqualTo(offers);

        verify(dealClient, times(3)).calculateLoanTerms(request);
    }

    @Test
    void serviceShouldRetryAndThrowWhenDealClientBodyIsNull() {
        LoanStatementRequestDto request = request();

        when(dealClient.calculateLoanTerms(request))
                .thenReturn(ResponseEntity.<List<LoanOfferDto>>ok().build());

        assertThatThrownBy(() -> service.calculateLoanOffers(request))
                .isInstanceOf(OffersNotFoundException.class)
                .hasMessage("Кредитные предложения не найдены");

        verify(dealClient, times(3)).calculateLoanTerms(request);
    }

    @Test
    void serviceShouldSendSelectedOfferToDealClient() {
        LoanOfferDto offer = offer("15.50");

        service.selectOffer(offer);

        verify(dealClient).selectOffer(offer);
    }

    @Test
    void serviceShouldPropagateDealClientExceptionOnCalculate() {
        LoanStatementRequestDto request = request();
        RuntimeException exception = new RuntimeException("deal-api is unavailable");

        when(dealClient.calculateLoanTerms(request))
                .thenThrow(exception);

        assertThatThrownBy(() -> service.calculateLoanOffers(request))
                .isSameAs(exception)
                .hasMessage("deal-api is unavailable");

        verify(dealClient).calculateLoanTerms(request);
    }

    @Test
    void serviceShouldPropagateDealClientExceptionOnSelectOffer() {
        LoanOfferDto offer = offer("15.50");
        RuntimeException exception = new RuntimeException("offer selection failed");

        doThrow(exception).when(dealClient).selectOffer(offer);

        assertThatThrownBy(() -> service.selectOffer(offer))
                .isSameAs(exception)
                .hasMessage("offer selection failed");

        verify(dealClient).selectOffer(offer);
    }

    @Test
    void loanOfferDtoShouldWorkCorrectly() {
        LoanOfferDto expected = offer("15.50");
        LoanOfferDto actual = copyOfferBySetters(expected);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        assertThat(actual)
                .isEqualTo(expected)
                .hasSameHashCodeAs(expected);

        assertThat(actual.toString())
                .contains("requestedAmount=300000")
                .contains("rate=15.50");
    }

    @Test
    void loanStatementRequestDtoShouldWorkCorrectly() {
        LoanStatementRequestDto expected = request();
        LoanStatementRequestDto actual = copyRequestBySetters(expected);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        assertThat(actual)
                .isEqualTo(expected)
                .hasSameHashCodeAs(expected);

        assertThat(actual.toString())
                .contains("amount=300000")
                .contains("firstName=Ivan")
                .contains("email=ivan.ivanov@mail.ru");
    }

    @Test
    void validationExceptionShouldContainMessage() {
        ValidationException exception = new ValidationException("invalid request");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("invalid request");
    }

    @Nested
    class DatabaseLockIntegrationTests {

        @Test
        void secondSelectOfferCallShouldWaitUntilFirstCallReleasesLock() throws Exception {
            LoanOfferDto offer = offer("15.50");

            CountDownLatch firstCallLockedStatement = new CountDownLatch(1);
            CountDownLatch releaseFirstCall = new CountDownLatch(1);

            Semaphore simulatedDbLock = new Semaphore(1);
            AtomicInteger callCounter = new AtomicInteger();

            doAnswer(invocation -> {
                simulatedDbLock.acquire();

                try {
                    int currentCall = callCounter.incrementAndGet();

                    if (currentCall == 1) {
                        firstCallLockedStatement.countDown();

                        assertThat(releaseFirstCall.await(5, SECONDS))
                                .as("Первый вызов должен дождаться разрешения на завершение")
                                .isTrue();
                    }

                    return ResponseEntity.ok().build();
                } finally {
                    simulatedDbLock.release();
                }
            }).when(dealClient).selectOffer(offer);

            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                Future<?> firstCall = executor.submit(() -> service.selectOffer(offer));

                assertThat(firstCallLockedStatement.await(5, SECONDS))
                        .as("Первый вызов должен заблокировать обработку оффера")
                        .isTrue();

                Future<?> secondCall = executor.submit(() -> service.selectOffer(offer));

                MILLISECONDS.sleep(500);

                assertThat(secondCall.isDone())
                        .as("Второй вызов должен ждать освобождения lock")
                        .isFalse();

                releaseFirstCall.countDown();

                firstCall.get(5, SECONDS);
                secondCall.get(5, SECONDS);

                assertThat(secondCall.isDone()).isTrue();

                verify(dealClient, times(2)).selectOffer(offer);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static LoanOfferDto copyOfferBySetters(LoanOfferDto expected) {
        LoanOfferDto actual = new LoanOfferDto();

        actual.setStatementId(expected.getStatementId());
        actual.setRequestedAmount(expected.getRequestedAmount());
        actual.setTotalAmount(expected.getTotalAmount());
        actual.setTerm(expected.getTerm());
        actual.setMonthlyPayment(expected.getMonthlyPayment());
        actual.setRate(expected.getRate());
        actual.setIsInsuranceEnabled(expected.getIsInsuranceEnabled());
        actual.setIsSalaryClient(expected.getIsSalaryClient());

        return actual;
    }

    private static LoanStatementRequestDto copyRequestBySetters(LoanStatementRequestDto expected) {
        LoanStatementRequestDto actual = new LoanStatementRequestDto();

        actual.setAmount(expected.getAmount());
        actual.setTerm(expected.getTerm());
        actual.setFirstName(expected.getFirstName());
        actual.setLastName(expected.getLastName());
        actual.setMiddleName(expected.getMiddleName());
        actual.setEmail(expected.getEmail());
        actual.setBirthdate(expected.getBirthdate());
        actual.setPassportSeries(expected.getPassportSeries());
        actual.setPassportNumber(expected.getPassportNumber());

        return actual;
    }

    private static LoanStatementRequestDto request() {
        return new LoanStatementRequestDto(
                bd("300000"),
                24,
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "ivan.ivanov@mail.ru",
                LocalDate.of(1995, 5, 15),
                "1234",
                "123456"
        );
    }

    private static List<LoanOfferDto> offers() {
        return List.of(
                offer("18.00"),
                offer("16.50"),
                offer("15.50"),
                offer("14.00")
        );
    }

    private static LoanOfferDto offer(String rate) {
        return new LoanOfferDto(
                UUID.randomUUID(),
                bd("300000"),
                bd("345000"),
                24,
                bd("15500"),
                bd(rate),
                true,
                false
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static FeignException feignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "/deal/statement",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );

        Response response = Response.builder()
                .status(status)
                .reason("test")
                .request(request)
                .headers(Map.of())
                .build();

        return FeignException.errorStatus("DealClient#request", response);
    }

    private static String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
}