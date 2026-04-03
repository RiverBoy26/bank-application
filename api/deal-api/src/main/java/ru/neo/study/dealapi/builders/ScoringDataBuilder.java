package ru.neo.study.dealapi.builders;

import dto.EmploymentDto;
import dto.FinishRegistrationRequestDto;
import dto.LoanOfferDto;
import dto.ScoringDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Passport;
import ru.neo.study.dealapi.entity.Statement;

@Component
@Slf4j
public class ScoringDataBuilder {
    public ScoringDataDto buildScoringData(Statement statement, FinishRegistrationRequestDto request) {
        Client client = statement.getClient();
        LoanOfferDto appliedOffer = statement.getAppliedOffer();

        if (appliedOffer == null) {
            log.error("Для statementId={} отсутствует выбранное предложение", statement.getId());
            throw new IllegalStateException("Для заявки не выбрано кредитное предложение");
        }

        Passport passport = client.getPassport();
        EmploymentDto employmentDto = request.getEmployment();

        return new ScoringDataDto(
                appliedOffer.getRequestedAmount(),
                appliedOffer.getTerm(),
                client.getFirstName(),
                client.getLastName(),
                client.getMiddleName(),
                request.getGender(),
                client.getBirthDate(),
                passport != null ? passport.getSeries() : null,
                passport != null ? passport.getNumber() : null,
                request.getPassportIssueDate(),
                request.getPassportIssueBranch(),
                request.getMaritalStatus(),
                request.getDependentAmount(),
                employmentDto,
                request.getAccountNumber(),
                appliedOffer.getIsInsuranceEnabled(),
                appliedOffer.getIsSalaryClient()
        );
    }
}
