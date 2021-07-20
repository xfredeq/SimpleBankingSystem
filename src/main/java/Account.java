import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Random;

public class Account {
    private final float balance;

    private final int BIN;
    private final long accountIdentifier;
    private final int checksum;
    private int PIN;

    private static long cardAccountIdentifier = 100_000_000;

    Account(SQLiteDataSource dataSource) {
        this.balance = 0;

        Random random = new Random();
        this.BIN = 400_000;

        boolean czy = true;

        do {
            String accountStatement = "SELECT * FROM card WHERE number LIKE ?";
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(accountStatement)) {

                    statement.setString(1, BIN + String.valueOf(Account.cardAccountIdentifier) +"%");

                    ResultSet resultSet = statement.executeQuery();

                    if (!resultSet.next()) {
                        czy = false;
                    } else {
                        Account.cardAccountIdentifier++;
                    }
                }
            } catch (SQLException e) {
                System.out.println(Account.cardAccountIdentifier);
                System.out.println("Database connection error 2");
                e.printStackTrace();
            }
        } while (czy);

        this.accountIdentifier = Account.cardAccountIdentifier;
        Account.cardAccountIdentifier++;

        this.PIN = 0;
        int multiplier = 1000;
        for (int i = 0; i < 4; i++) {
            int tmp = random.nextInt(10);
            while (tmp == 0) {
                tmp = random.nextInt(10);
            }
            this.PIN += tmp * multiplier;
            multiplier /= 10;
        }

        this.checksum = calculateChecksum();

        this.createCard(dataSource);
    }

    public void createCard(SQLiteDataSource dataSource) {
        String insertStatement = "INSERT INTO card (number, pin, balance) VALUES(?, ?, ?)";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(insertStatement)) {

                statement.setString(1, this.getCardNumber());
                statement.setString(2, this.getPIN());
                statement.setFloat(3, this.balance);

                statement.executeUpdate();

                System.out.printf("\nYour card has been created\nYour card number:\n" +
                        "%s\nYour card PIN:\n%s\n", this.getCardNumber(), this.getPIN());

            }
        } catch (SQLException e) {
            System.out.println(Account.cardAccountIdentifier);
            System.out.println("Database connection error 2");
            e.printStackTrace();
        }
    }

    public String getCardNumber() {
        return BIN + String.valueOf(accountIdentifier) + checksum;
    }

    public String getPIN ( )
    {
        if (this.PIN < 1000) {
            return String.valueOf(0) + PIN;
        } else {
            return String.valueOf(PIN);
        }
    }

    private int calculateChecksum() {

        long s = 8 + getSum(this.accountIdentifier);
        return (int)((s / 10 * 10 + 10 - s) % 10);
    }

    public static boolean checkChecksum(String cardNumber) {
        long acID = Long.parseLong(cardNumber.substring(0,15));

        long s = getSum(acID);
        int cs = (int)((s / 10 * 10 + 10 - s) % 10);
        return cs == Integer.parseInt(cardNumber.substring(15,16));
    }

    private static long getSum (long acID)
    {
        long s = 0;
        boolean czy = true;

        while (acID > 0) {
            long pom = acID % 10;
            if (czy) {
                pom *= 2;
            }
            czy = !czy;
            if (pom > 9) {
                pom -= 9;
            }
            //System.out.println(pom);
            s += pom;
            //System.out.println(s);
            acID /= 10;
        }
        return s;
    }
}
