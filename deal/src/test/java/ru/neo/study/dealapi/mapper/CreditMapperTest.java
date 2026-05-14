package ru.neo.study.dealapi.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.neo.study.dealapi.dto.CreditDto;
import ru.neo.study.dealapi.entity.Credit;
import ru.neo.study.dealapi.enums.CreditStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreditMapperTest {

    private final CreditMapper mapper = Mappers.getMapper(CreditMapper.class);

    @Test
    void toEntityShouldMapCreditDtoToCredit() {
        var dto = mock(CreditDto.class);

        when(dto.getIsInsuranceEnabled()).thenReturn(true);
        when(dto.getIsSalaryClient()).thenReturn(false);

        Credit credit = mapper.toEntity(dto);

        assertThat(credit.getInsuranceEnabled()).isTrue();
        assertThat(credit.getSalaryClient()).isFalse();
        assertThat(credit.getCreditStatus()).isEqualTo(CreditStatus.CALCULATED);
    }
}