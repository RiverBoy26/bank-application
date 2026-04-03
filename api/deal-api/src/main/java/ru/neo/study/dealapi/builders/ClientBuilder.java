package ru.neo.study.dealapi.builders;

import dto.EmploymentDto;
import dto.FinishRegistrationRequestDto;
import dto.LoanStatementRequestDto;
import org.springframework.stereotype.Component;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Employment;
import ru.neo.study.dealapi.entity.Passport;

@Component
public class ClientBuilder {
    public Client buildClient(LoanStatementRequestDto request) {
        Passport passport = Passport.builder()
                .series(request.getPassportSeries())
                .number(request.getPassportNumber())
                .build();

        return Client.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .email(request.getEmail())
                .birthDate(request.getBirthdate())
                .passport(passport)
                .build();
    }

    public void enrichClient(Client client, FinishRegistrationRequestDto request) {
        client.setGender(request.getGender());
        client.setMaritalStatus(request.getMaritalStatus());
        client.setDependentAmount(request.getDependentAmount());
        client.setAccountNumber(request.getAccountNumber());

        Passport passport = client.getPassport() == null ? new Passport() : client.getPassport();
        passport.setIssueDate(request.getPassportIssueDate());
        passport.setIssueBranch(request.getPassportIssueBranch());
        client.setPassport(passport);

        EmploymentDto employmentDto = request.getEmployment();
        if (employmentDto != null) {
            Employment employment = Employment.builder()
                    .status(employmentDto.getEmploymentStatus())
                    .employerInn(employmentDto.getEmployerINN())
                    .salary(employmentDto.getSalary())
                    .position(employmentDto.getPosition())
                    .workExperienceTotal(employmentDto.getWorkExperienceTotal())
                    .workExperienceCurrent(employmentDto.getWorkExperienceCurrent())
                    .build();
            client.setEmployment(employment);
        }
    }
}
