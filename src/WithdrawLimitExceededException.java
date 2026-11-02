/** Превышен дневной лимит на снятие/перевод. */
public class WithdrawLimitExceededException extends RuntimeException {
    public WithdrawLimitExceededException(String message) {
        super(message);
    }
}
