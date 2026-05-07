package ru.neo.study.statement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.neo.study.statement.dealClient.DealClient;
import ru.neo.study.statement.dto.LoanOfferDto;
import ru.neo.study.statement.dto.LoanStatementRequestDto;
import ru.neo.study.statement.exception.OffersNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementServiceTests {

    @Mock
    private DealClient dealClient;

    private StatementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StatementServiceImpl(dealClient);
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
    void serviceShouldThrowWhenDealClientBodyIsNull() {
        LoanStatementRequestDto request = request();

        when(dealClient.calculateLoanTerms(request))
                .thenReturn(ResponseEntity.<List<LoanOfferDto>>ok().build());

        assertThatThrownBy(() -> service.calculateLoanOffers(request))
                .isInstanceOf(OffersNotFoundException.class)
                .hasMessage("Кредитные предложения не найдены");

        verify(dealClient).calculateLoanTerms(request);
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

}