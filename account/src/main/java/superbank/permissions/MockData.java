package superbank.permissions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import superbank.permissions.entities.Account;
import superbank.permissions.entities.Transaction;

import java.time.Instant;

import static superbank.permissions.entities.TransactionResult.*;
import static superbank.permissions.entities.TransactionType.OUTGOING;

public class MockData {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository tr;

    private static Logger log = LoggerFactory.getLogger(MockData.class);

    public void initialize() {
        Account account1 = accountRepository.save(new Account("EU12345435345435345", 1, "EU"));
        log.info("Mock account IBAN: {}", account1.getIban());
        Account account2 = accountRepository.save(new Account("US12345435345444444", 1, "US"));
        tr.save(new Transaction("SK54354656343444", 135.2, account1, Instant.parse("2021-12-05T10:15:30.00Z"), OUTGOING, SUCCESS, null));
        tr.save(new Transaction("SK54354656343444", 115.5, account1, Instant.parse("2021-12-02T10:15:30.00Z"), OUTGOING, SUCCESS, null));
        tr.save(new Transaction("SK54354656343444",  35.0, account1, Instant.parse("2021-12-01T08:15:30.00Z"), OUTGOING, SUCCESS, null));
        tr.save(new Transaction("SK54354656343444",  900.5, account1, Instant.parse("2021-11-02T10:15:30.00Z"), OUTGOING, SUCCESS, null));
        tr.save(new Transaction("SK54354656343444",  122.1, account1, Instant.parse("2021-11-05T10:15:30.00Z"), OUTGOING, FAILURE, "Technical Error"));
        tr.save(new Transaction("SK54354656343444",  135.2, account1, Instant.parse("2021-11-11T10:15:30.00Z"), OUTGOING, FAILURE, "Lack of Funds"));
        tr.save(new Transaction("SK54354656343444",  135.2, account1, Instant.parse("2021-11-15T10:15:30.00Z"), OUTGOING, FAILURE, "Lack of Funds"));
    }
}
