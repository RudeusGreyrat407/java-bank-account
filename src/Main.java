public class Main {
    public static void main(String[] args) {
        BankAccount alice = new BankAccount("ACC-1", "Алиса");
        BankAccount bob = new BankAccount("ACC-2", "Боб", Currency.USD, new Balance(50_000, Currency.USD));

        System.out.println("=== 1. Пополнение ===");
        alice.deposit(Balance.ofRubles(50_000));
        bob.deposit(new Balance(100_000, Currency.USD));        // 1000 $
        System.out.println(alice);
        System.out.println(bob);

        System.out.println("\n=== 2. Неизменяемость Balance ===");
        Balance a = Balance.ofRubles(100);
        Balance b = a.add(Balance.ofRubles(50));
        System.out.println("a = " + a + ", a.add(50) = " + b + " (a не изменился)");

        System.out.println("\n=== 3. Конвертация ===");
        Balance rub = Balance.ofRubles(9_600);
        System.out.println(rub + " = " + rub.convertTo(Currency.USD));
        System.out.println(rub + " = " + rub.convertTo(Currency.EUR) + " (округление вниз)");
        System.out.println(rub + " = " + rub.convertTo(Currency.CNY));

        System.out.println("\n=== 4. Перевод между счетами (RUB -> USD счёт) ===");
        alice.transferTo(bob, Balance.ofRubles(9_600));          // Бобу придёт 100 $
        System.out.println(alice);
        System.out.println(bob);

        System.out.println("\n=== 5. Корнер кейсы (все должны упасть с исключением) ===");
        tryIt("Отрицательный баланс", () -> new Balance(-1, Currency.RUB));
        tryIt("Баланс с null-валютой", () -> new Balance(10, null));
        tryIt("Прибавить отрицательную/нулевую сумму", () -> a.add(new Balance(0, Currency.RUB)));
        tryIt("Прибавить null", () -> a.add(null));
        tryIt("Сложить рубли и доллары", () -> a.add(new Balance(100, Currency.USD)));
        tryIt("Вычесть больше, чем есть", () -> a.subtract(Balance.ofRubles(1_000)));
        BankAccount carol = new BankAccount("ACC-3", "Кэрол");
        carol.deposit(Balance.ofRubles(100));
        tryIt("Снять больше, чем на счёте", () -> carol.withdraw(Balance.ofRubles(1_000)));
        tryIt("Снять 0", () -> carol.withdraw(new Balance(0, Currency.RUB)));
        tryIt("Перевод самому себе", () -> alice.transferTo(alice, Balance.ofRubles(1)));
        tryIt("Перевод на null", () -> alice.transferTo(null, Balance.ofRubles(1)));
        tryIt("Счёт с пустым владельцем", () -> new BankAccount("X", "  "));
        tryIt("Переполнение при сложении", () -> new Balance(Long.MAX_VALUE, Currency.RUB)
                .add(new Balance(1, Currency.RUB)));

        System.out.println("\n=== 6. Дневной лимит ===");
        // у Алисы лимит 100 000 ₽, уже списано 9 600 ₽
        alice.deposit(Balance.ofRubles(200_000));
        tryIt("Снять 95 000 ₽ (лимит превышен)", () -> alice.withdraw(Balance.ofRubles(95_000)));
        alice.withdraw(Balance.ofRubles(90_000));
        System.out.println("Снято 90 000 ₽ -> " + alice);
        alice.resetDailyLimit();
        alice.withdraw(Balance.ofRubles(95_000));
        System.out.println("После сброса лимита снято ещё 95 000 ₽ -> " + alice);

        System.out.println("\n=== 7. Заморозка счёта ===");
        bob.freeze();
        tryIt("Перевод на замороженный счёт", () -> alice.transferTo(bob, Balance.ofRubles(100)));
        tryIt("Снятие с замороженного счёта", () -> bob.withdraw(new Balance(100, Currency.USD)));
        bob.unfreeze();
        alice.deposit(Balance.ofRubles(1_000));
        alice.transferTo(bob, Balance.ofRubles(960));
        System.out.println(bob);

        System.out.println("\n=== 8. История операций Алисы ===");
        alice.getHistory().forEach(System.out::println);
    }

    /** Выполняет действие и печатает, какое исключение оно выбросило. */
    private static void tryIt(String title, Runnable action) {
        try {
            action.run();
            System.out.println("[!] " + title + ": исключения НЕ было");
        } catch (RuntimeException e) {
            System.out.println("[ok] " + title + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
