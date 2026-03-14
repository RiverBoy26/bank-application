package ru.neo.study.calculator.mapper;

import lombok.experimental.UtilityClass;
import ru.neo.study.calculator.dto.LoanStatementRequestDto;
import ru.neo.study.calculator.model.LoanStatementRequest;

@UtilityClass
public class LoanStatementRequestMapper {
    public LoanStatementRequestDto toLoanStatementRequestDto(LoanStatementRequest loanStatementRequest) {
        return new LoanStatementRequestDto(
                loanStatementRequest.getAmount(),
                loanStatementRequest.getTerm(),
                loanStatementRequest.getFirstName(),
                loanStatementRequest.getLastName(),
                loanStatementRequest.getMiddleName(),
                loanStatementRequest.getEmail(),
                loanStatementRequest.getBirthdate(),
                loanStatementRequest.getPassportSeries(),
                loanStatementRequest.getPassportNumber()
        );
    }

    public LoanStatementRequest toLoanStatementRequest(LoanStatementRequestDto loanStatementRequestDto) {
        LoanStatementRequest loanStatementRequest = new LoanStatementRequest();

        loanStatementRequest.setAmount(loanStatementRequestDto.getAmount());
        loanStatementRequest.setTerm(loanStatementRequestDto.getTerm());
        loanStatementRequest.setFirstName(loanStatementRequestDto.getFirstName());
        loanStatementRequest.setLastName(loanStatementRequestDto.getLastName());
        loanStatementRequest.setMiddleName(loanStatementRequestDto.getMiddleName());
        loanStatementRequest.setEmail(loanStatementRequestDto.getEmail());
        loanStatementRequest.setBirthdate(loanStatementRequestDto.getBirthdate());
        loanStatementRequest.setPassportSeries(loanStatementRequestDto.getPassportSeries());
        loanStatementRequest.setPassportNumber(loanStatementRequestDto.getPassportNumber());

        return loanStatementRequest;
    }
}
