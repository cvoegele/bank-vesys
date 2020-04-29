package bank.graphql.client.models;

import bank.graphql.models.Account;

/**
 * Simple Model Class to fill in with the GraphQL Response as Json. Parsed by GSON Library
 * This class is filled by every response with different values. The values that are irrelevant for a certain response are then simply null
 */
public class Data {
    //getAccounts --> Array of account numbers
    public Number[] accounts;
    //create Account --> Contains account number
    public Number createAccount;
    //close Account --> indicates whether closing was successful or not
    public boolean closeAccount;
    //deposit, withdraw, transfer --> contains "true" or the thrown Exception name as a String
    public String deposit;
    public String withdraw;
    public String transfer;
    //getNumber, getOwner, getBalance, getActive --> returns Account Object with only asked (corresponding to request query) value filled in
    public Account account;
}
