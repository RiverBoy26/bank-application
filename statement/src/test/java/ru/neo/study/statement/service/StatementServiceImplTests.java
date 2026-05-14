package ru.neo.study.statement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.neo.study.statement.dealClient.DealClient;
import ru.neo.study.statement.dto.LoanOfferDto;
import ru.neo.study.statement.dto.LoanStatementRequestDto;
import ru.neo.study.statement.exception.OffersNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementServiceImplTests {

    @Mock
    private DealClient dealClient;

    @InjectMocks
    private StatementServiceImpl service;

    @Test
    void calculateLoanOffersShouldReturnOffers() {
        LoanStatementRequestDto request = mock(LoanStatementRequestDto.class);
        List<LoanOfferDto> offers = List.of(mock(LoanOfferDto.class));

        when(dealClient.calculateLoanTerms(request))
                .thenReturn(ResponseEntity.ok(offers));

        List<LoanOfferDto> result = service.calculateLoanOffers(request);

        assertThat(result).isSameAs(offers);

        verify(dealClient).calculateLoanTerms(request);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void calculateLoanOffersShouldThrowWhenOffersAreMissing(List<LoanOfferDto> offers) {
        LoanStatementRequestDto request = mock(LoanStatementRequestDto.class);

        when(dealClient.calculateLoanTerms(request))
                .thenReturn(ResponseEntity.ok(offers));

        assertThatThrownBy(() -> service.calculateLoanOffers(request))
                .isInstanceOf(OffersNotFoundException.class)
                .hasMessage("Кредитные предложения не найдены");
    }

    @Test
    void selectOfferShouldSendOfferToDealClient() {
        LoanOfferDto offer = mock(LoanOfferDto.class);

        service.selectOffer(offer);

        verify(dealClient).selectOffer(offer);
    }
}