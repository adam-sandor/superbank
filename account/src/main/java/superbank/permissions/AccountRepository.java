package superbank.permissions;

import org.springframework.data.repository.CrudRepository;
import superbank.permissions.entities.Account;

import java.util.Optional;

public interface AccountRepository extends CrudRepository<Account, Long> {

    Optional<Account> findAccountByIban(String iban);
}
