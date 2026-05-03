package ru.neo.study.dealapi.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.neo.study.dealapi.entity.Statement;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatementRepository extends JpaRepository<Statement, UUID> {
    @Lock(LockModeType.OPTIMISTIC)
    @Query("select s from Statement s where s.id = :statementId")
    Optional<Statement> findByIdWithBlock(@Param("statementId") UUID statementId);
}
