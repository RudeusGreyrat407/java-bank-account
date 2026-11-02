import java.util.Objects;

/**
 * Неизменяемая денежная сумма: количество минимальных единиц (копеек/центов)
 * и валюта. Любая операция возвращает НОВЫЙ объект Balance.
 * Инвариант: сумма никогда не отрицательна.
 */
public final class Balance {
    /** В каждой валюте из Currency 1 основная единица = 100 минимальных. */
    private static final long MINOR_UNITS_IN_MAJOR = 100;

    private final long amountInKopecks;
    private final Currency currency;

    public Balance(long amountInKopecks, Currency currency) {
        this.currency = Objects.requireNonNull(currency, "currency не может быть null");
        if (amountInKopecks < 0) {
            throw new IllegalArgumentException("Баланс не может быть отрицательным: " + amountInKopecks);
        }
        this.amountInKopecks = amountInKopecks;
    }

    /** Нулевой баланс в рублях. */
    public static Balance zeroRub() {
        return new Balance(0, Currency.RUB);
    }

    /** Баланс из целых рублей. */
    public static Balance ofRubles(long rubles) {
        return new Balance(Math.multiplyExact(rubles, MINOR_UNITS_IN_MAJOR), Currency.RUB);
    }

    public long getAmountInKopecks() {
        return amountInKopecks;
    }

    public Currency getCurrency() {
        return currency;
    }

    public boolean isZero() {
        return amountInKopecks == 0;
    }

    /** Прибавить сумму. Нельзя прибавлять null, 0 и сумму в другой валюте. */
    public Balance add(Balance other) {
        requireSameCurrency(other);
        requirePositive(other);
        try {
            return new Balance(Math.addExact(amountInKopecks, other.amountInKopecks), currency);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Переполнение при сложении балансов", e);
        }
    }

    /** Вычесть сумму. Нельзя уйти в минус. */
    public Balance subtract(Balance other) {
        requireSameCurrency(other);
        requirePositive(other);
        if (other.amountInKopecks > amountInKopecks) {
            throw new InsufficientFundsException(
                    "Недостаточно средств: доступно " + this + ", требуется " + other);
        }
        return new Balance(amountInKopecks - other.amountInKopecks, currency);
    }

    /**
     * Конвертация в другую валюту по курсу из Currency.
     * Остаток от деления отбрасывается (округление вниз до копейки).
     * Формула: сумма * цена_исходной / цена_целевой.
     */
    public Balance convertTo(Currency target) {
        Objects.requireNonNull(target, "target не может быть null");
        if (target == currency) {
            return this;
        }
        try {
            long converted = Math.multiplyExact(amountInKopecks, currency.getPriceInRublesKopecks())
                    / target.getPriceInRublesKopecks();
            return new Balance(converted, target);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Переполнение при конвертации " + this + " в " + target, e);
        }
    }

    public boolean isGreaterThan(Balance other) {
        requireSameCurrency(other);
        return amountInKopecks > other.amountInKopecks;
    }

    public Balance min(Balance other) {
        requireSameCurrency(other);
        return amountInKopecks <= other.amountInKopecks ? this : other;
    }

    private void requireSameCurrency(Balance other) {
        Objects.requireNonNull(other, "other не может быть null");
        if (other.currency != currency) {
            throw new IllegalArgumentException(
                    "Разные валюты: " + currency + " и " + other.currency + " (используйте convertTo)");
        }
    }

    private static void requirePositive(Balance other) {
        if (other.amountInKopecks <= 0) {
            throw new IllegalArgumentException("Сумма операции должна быть положительной: " + other);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Balance)) return false;
        Balance that = (Balance) o;
        return amountInKopecks == that.amountInKopecks && currency == that.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountInKopecks, currency);
    }

    @Override
    public String toString() {
        return String.format("%d.%02d %s",
                amountInKopecks / MINOR_UNITS_IN_MAJOR,
                amountInKopecks % MINOR_UNITS_IN_MAJOR,
                currency.getSymbol());
    }
}
