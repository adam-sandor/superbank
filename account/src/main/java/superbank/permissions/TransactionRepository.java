package superbank.permissions;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import superbank.permissions.entities.Transaction;
import superbank.permissions.entities.TransactionResult;
import superbank.permissions.entities.TransactionType;

import java.util.List;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t WHERE (:result is null or t.result = :result) and (:type is null"
            + " or t.type = :type) ORDER BY t.timeStamp DESC")
    List<Transaction> findTransactions(@Param("result") TransactionResult result, @Param("type") TransactionType type);
}
