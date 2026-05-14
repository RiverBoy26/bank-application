package ru.neo.study.dealapi.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.neo.study.dealapi.dto.EmploymentDto;
import ru.neo.study.dealapi.dto.FinishRegistrationRequestDto;
import ru.neo.study.dealapi.dto.LoanStatementRequestDto;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.enums.EmploymentStatus;
import ru.neo.study.dealapi.enums.Gender;
import ru.neo.study.dealapi.enums.MaritalStatus;
import ru.neo.study.dealapi.enums.Position;
import ru.neo.study.dealapi.jsonb.Employment;
import ru.neo.study.dealapi.jsonb.Passport;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientMapperTest {

    private final ClientMapper mapper = Mappers.getMapper(ClientMapper.class);

    @Test
    void toEntityShouldMapLoanStatementRequestToClient() {
        LoanStatementRequestDto request = mock(LoanStatementRequestDto.class);

        when(request.getFirstName()).thenReturn("Ivan");
        when(request.getBirthdate()).thenReturn(LocalDate.of(1995, 5, 20));
        when(request.getPassportSeries()).thenReturn("1234");
        when(request.getPassportNumber()).thenReturn("567890");

        Client client = mapper.toEntity(request);

        assertThat(client.getFirstName()).isEqualTo("Ivan");
        assertThat(client.getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 20));
        assertThat(client.getPassport().getSeries()).isEqualTo("1234");
        assertThat(client.getPassport().getNumber()).isEqualTo("567890");
    }

    @Test
    void updateClientFromFinishRegistrationShouldUpdateClient() {
        Client client = new Client();

        FinishRegistrationRequestDto request = mock(FinishRegistrationRequestDto.class);
        EmploymentDto employment = new EmploymentDto(
                EmploymentStatus.EMPLOYED,
                "7701234567",
                new BigDecimal("150000"),
                Position.MID_MANAGEMENT,
                120,
                36
        );

        when(request.getGender()).thenReturn(Gender.MALE);
        when(request.getMaritalStatus()).thenReturn(MaritalStatus.MARRIED);
        when(request.getEmployment()).thenReturn(employment);
        when(request.getPassportIssueDate()).thenReturn(LocalDate.of(2015, 4, 12));
        when(request.getPassportIssueBranch()).thenReturn("ОВД 770-001");

        mapper.updateClientFromFinishRegistration(request, client);

        assertThat(client.getGender()).isEqualTo(Gender.MALE);
        assertThat(client.getMaritalStatus()).isEqualTo(MaritalStatus.MARRIED);
        assertThat(client.getEmployment().getStatus()).isEqualTo(EmploymentStatus.EMPLOYED);
        assertThat(client.getPassport().getIssueBranch()).isEqualTo("ОВД 770-001");
    }

    @Test
    void toPassportShouldMapPassportData() {
        LoanStatementRequestDto request = mock(LoanStatementRequestDto.class);

        when(request.getPassportSeries()).thenReturn("1234");
        when(request.getPassportNumber()).thenReturn("567890");

        Passport passport = mapper.toPassport(request);

        assertThat(passport.getSeries()).isEqualTo("1234");
        assertThat(passport.getNumber()).isEqualTo("567890");
        assertThat(passport.getId()).isNotNull();
    }

    @Test
    void toEmploymentShouldMapEmploymentData() {
        var dto = mock(EmploymentDto.class);

        when(dto.getEmploymentStatus()).thenReturn(EmploymentStatus.EMPLOYED);
        when(dto.getEmployerINN()).thenReturn("7701234567");

        Employment employment = mapper.toEmployment(dto);

        assertThat(employment.getStatus()).isEqualTo(EmploymentStatus.EMPLOYED);
        assertThat(employment.getEmployerInn()).isEqualTo("7701234567");
        assertThat(employment.getId()).isNotNull();
    }
}