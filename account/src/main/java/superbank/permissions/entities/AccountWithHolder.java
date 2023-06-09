package superbank.permissions.entities;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;

public class AccountWithHolder {

    private String iban;
    private AccountHolder accountHolder;
    private String geoRegion;

    public AccountWithHolder(Account account, AccountHolder accountHolder) {
        this.iban = account.getIban();
        this.accountHolder = accountHolder;
        this.geoRegion = account.getGeoRegion();
    }

    public AccountWithHolder(String iban, AccountHolder accountHolder, String geoRegion) {
        this.iban = iban;
        this.accountHolder = accountHolder;
        this.geoRegion = geoRegion;
    }

    public String getIban() {
        return iban;
    }

    public AccountHolder getAccountHolder() {
        return accountHolder;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getGeoRegion() {
        return geoRegion;
    }
}