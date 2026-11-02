import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Банковский счёт. Баланс неизменяем (Balance), поэтому каждое изменение
 * заменяет объект в поле balance на новый.
 *
 * Дополнительные поля и операции:
 *  - дневной лимит на списание (dailyWithdrawLimit) + сколько уже списано сегодня;
 *  - заморозка счёта (frozen): все операции запрещены;
 *  - история операций (history).
 */
public class BankAccount {
    private final String id;
    private final String owner;
    private final Currency currency;
    private final Balance dailyWithdrawLimit;
    private final List<String> history = new ArrayList<>();

    private Balance balance;
    private Balance withdrawnToday;
    private boolean frozen;

    public BankAccount(String id, String owner, Currency currency, Balance dailyWithdrawLimit) {
        this.id = requireNotBlank(id, "id");
        this.owner = requireNotBlank(owner, "owner");
        this.currency = Objects.requireNonNull(currency, "currency не может быть null");
        Objects.requireNonNull(dailyWithdrawLimit, "dailyWithdrawLimit не может быть null");
        if (dailyWithdrawLimit.isZero()) {
            throw new IllegalArgumentException("Лимит должен быть больше нуля");
        }
        this.dailyWithdrawLimit = dailyWithdrawLimit.convertTo(currency);
        this.balance = new Balance(0, currency);
        this.withdrawnToday = new Balance(0, currency);
    }

    public BankAccount(String id, String owner) {
        this(id, owner, Currency.RUB, Balance.ofRubles(100_000));
    }

    // ---------- операции ----------

    /** Пополнение. Сумма может быть в любой валюте: конвертируется в валюту счёта. */
    public void deposit(Balance amount) {
        requireNotFrozen();
        Objects.requireNonNull(amount, "amount не может быть null");
        Balance converted = amount.convertTo(currency);
        balance = balance.add(converted);
        log("Пополнение +" + converted);
    }

    /** Снятие с учётом баланса и дневного лимита. */
    public void withdraw(Balance amount) {
        requireNotFrozen();
        Objects.requireNonNull(amount, "amount не может быть null");
        Balance converted = amount.convertTo(currency);
        checkLimit(converted);
        balance = balance.subtract(converted);      // бросит исключение, если не хватает денег
        withdrawnToday = withdrawnToday.add(converted);
        log("Снятие -" + converted);
    }

    /**
     * Перевод на другой счёт. Сумма указывается в любой валюте,
     * у отправителя списывается в его валюте, получателю зачисляется в его.
     * Все проверки выполняются ДО изменения состояния, поэтому перевод атомарен.
     */
    public void transferTo(BankAccount target, Balance amount) {
        Objects.requireNonNull(target, "target не может быть null");
        Objects.requireNonNull(amount, "amount не может быть null");
        if (target == this || target.id.equals(this.id)) {
            throw new IllegalArgumentException("Нельзя переводить деньги самому себе");
        }
        requireNotFrozen();
        target.requireNotFrozen();

        Balance debit = amount.convertTo(currency);
        Balance credit = amount.convertTo(target.currency);
        if (debit.isZero() || credit.isZero()) {
            throw new IllegalArgumentException("Сумма слишком мала для перевода: " + amount);
        }
        checkLimit(debit);
        Balance newBalance = balance.subtract(debit);   // проверки пройдены -> меняем состояние
        Balance newTargetBalance = target.balance.add(credit);

        balance = newBalance;
        withdrawnToday = withdrawnToday.add(debit);
        target.balance = newTargetBalance;

        log("Перевод -" + debit + " -> " + target.id);
        target.log("Перевод +" + credit + " <- " + id);
    }

    public void freeze() {
        frozen = true;
        log("Счёт заморожен");
    }

    public void unfreeze() {
        frozen = false;
        log("Счёт разморожен");
    }

    /** Сброс дневного лимита (в реальности вызывался бы раз в сутки). */
    public void resetDailyLimit() {
        withdrawnToday = new Balance(0, currency);
        log("Дневной лимит сброшен");
    }

    // ---------- геттеры ----------

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public Currency getCurrency() { return currency; }
    public Balance getBalance() { return balance; }          // Balance неизменяем - отдавать безопасно
    public Balance getDailyWithdrawLimit() { return dailyWithdrawLimit; }
    public Balance getWithdrawnToday() { return withdrawnToday; }
    public boolean isFrozen() { return frozen; }

    public List<String> getHistory() {
        return Collections.unmodifiableList(history);
    }

    // ---------- вспомогательное ----------

    private void checkLimit(Balance amount) {
        Balance newTotal = withdrawnToday.add(amount);
        if (newTotal.isGreaterThan(dailyWithdrawLimit)) {
            throw new WithdrawLimitExceededException(
                    "Превышен дневной лимит " + dailyWithdrawLimit
                            + " (уже списано " + withdrawnToday + ", пытаются ещё " + amount + ")");
        }
    }

    private void requireNotFrozen() {
        if (frozen) {
            throw new IllegalStateException("Счёт " + id + " заморожен");
        }
    }

    private void log(String record) {
        history.add(record);
    }

    private static String requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name + " не может быть null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " не может быть пустым");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankAccount)) return false;
        return id.equals(((BankAccount) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("BankAccount{id=%s, владелец=%s, баланс=%s, списано сегодня=%s из %s%s}",
                id, owner, balance, withdrawnToday, dailyWithdrawLimit, frozen ? ", ЗАМОРОЖЕН" : "");
    }
}
