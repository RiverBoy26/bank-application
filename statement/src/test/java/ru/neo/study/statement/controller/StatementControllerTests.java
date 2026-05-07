package ru.neo.study.statement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.neo.study.statement.dto.LoanOfferDto;
import ru.neo.study.statement.dto.LoanStatementRequestDto;
import ru.neo.study.statement.exception.ErrorHandler;
import ru.neo.study.statement.exception.OfferSelectionConflictException;
import ru.neo.study.statement.exception.OffersNotFoundException;
import ru.neo.study.statement.service.StatementService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StatementControllerTests {

    @Mock
    private StatementService statementService;

    private MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StatementController(statementService))
                .setControllerAdvice(new ErrorHandler())
                .build();
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
                          "error": "STATEMENT_OFFERS_NOT_FOUND",
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
                          "error": "STATEMENT_OFFER_SELECTION_CONFLICT",
                          "description": "Заявка была изменена другим запросом. Обновите данные и повторите попытку."
                        }
                        """));
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

    private static String json(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
}
