package ru.neo.study.dealapi.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.neo.study.dealapi.dto.FinishRegistrationRequestDto;
import ru.neo.study.dealapi.dto.LoanOfferDto;
import ru.neo.study.dealapi.dto.ScoringDataDto;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.enums.Gender;
import ru.neo.study.dealapi.jsonb.Passport;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScoringDataMapperTest {

    private final ScoringDataMapper mapper = Mappers.getMapper(ScoringDataMapper.class);

    @Test
    void toDtoShouldMapScoringData() {
        Client client = mock(Client.class);
        Statement statement = mock(Statement.class);
        FinishRegistrationRequestDto request = mock(FinishRegistrationRequestDto.class);
        LoanOfferDto offer = mock(LoanOfferDto.class);
        Passport passport = mock(Passport.class);

        when(statement.getAppliedOffer()).thenReturn(offer);
        when(offer.getRequestedAmount()).thenReturn(new BigDecimal("300000"));
        when(offer.getTerm()).thenReturn(24);

        when(client.getFirstName()).thenReturn("Ivan");
        when(client.getPassport()).thenReturn(passport);
        when(passport.getSeries()).thenReturn("1234");

        when(request.getGender()).thenReturn(Gender.MALE);

        ScoringDataDto dto = mapper.toDto(client, statement, request);

        assertThat(dto.getAmount()).isEqualByComparingTo("300000");
        assertThat(dto.getTerm()).isEqualTo(24);
        assertThat(dto.getFirstName()).isEqualTo("Ivan");
        assertThat(dto.getPassportSeries()).isEqualTo("1234");
        assertThat(dto.getGender()).isEqualTo(Gender.MALE);
    }
}