package bank.network.UDP;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

public class BankPackage implements Serializable {

    public int res;

    //request parameter
    public String command;
    public String account;
    public String counterAccount;
    public Double amount;
    public Date date; //fully identifies a request to certain time, else you could not send the same request after another

    //response parameter
    public boolean status = true; //if request was generally ok
    public String response; //costum response message per request
    public Exception e; //transmit any server exceptions
    public Set<String> accountNumbers; //only for GET_ACCOUNTS request

    public BankPackage(String command, String account, String counterAccount, Double amount) {
        this.command = command;
        this.account = account;
        this.counterAccount = counterAccount;
        this.amount = amount;
        res = 0;
        date = new Date();
    }

    public void incrementRes() {
        res++;
    }

    /* this has response as response parameter? */
    public boolean isResponse(BankPackage response) {
        //check if its the response to the request --> is so when all attributes equal and res + 1
        return this.equals(response) && res + 1 == response.res;
    }

    public String getCommand() {
        return command;
    }

    public String getAccount() {
        return account;
    }

    public String getCounterAccount() {
        return counterAccount;
    }

    public Double getAmount() {
        return amount;
    }
    public boolean isOk() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Set<String> getAccountNumbers() {
        return accountNumbers;
    }

    public void setAccountNumbers(Set<String> accountNumbers) {
        this.accountNumbers = accountNumbers;
    }

    public Exception getE() {
        return e;
    }

    public void setE(Exception e) {
        this.e = e;
    }

    public int getRes() {
        return res;
    }

    public void setRes(int res) {
        this.res = res;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setCounterAccount(String counterAccount) {
        this.counterAccount = counterAccount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankPackage that = (BankPackage) o;
        return command.equals(that.command) &&
                Objects.equals(account, that.account) &&
                Objects.equals(counterAccount, that.counterAccount) &&
                Objects.equals(amount, that.amount) &&
                date.equals(that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, account, counterAccount, amount, date);
    }

    @Override
    public String toString() {
        return "BankPackage{" +
                "res=" + res +
                ", command='" + command + '\'' +
                ", account='" + account + '\'' +
                ", counterAccount='" + counterAccount + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                ", status=" + status +
                ", response='" + response + '\'' +
                ", e=" + e +
                ", accountNumbers=" + accountNumbers +
                '}';
    }
}
