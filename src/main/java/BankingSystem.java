import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class BankingSystem {
    private final Scanner scanner;
    private final SQLiteDataSource dataSource;


    private static final String[] promptingMessages = {
            "\n1. Create an account\n" +
                    "2. Log into account\n" +
                    "0. Exit\n",
            ""
    };

    private static final String[] loggedMessages = {
            "\n1. Balance\n" +
                    "2. Add income\n" +
                    "3. Do transfer\n" +
                    "4. Close account\n" +
                    "5. Log out\n" +
                    "0. Exit\n",
            ""
    };

    private static final String[] errorMessages = {
            "\nError, bad action!",
            ""
    };

    BankingSystem(String[] args) {
        this.scanner = new Scanner(System.in);
        this.dataSource = new SQLiteDataSource();

        String dbName = getDBName(args);

        this.createDatabase(dbName);
    }

    public void run() {
        int action;
        do {
            System.out.println(BankingSystem.promptingMessages[0]);
            try {
                action = scanner.nextInt();
                switch (action) {

                    case 0:
                        break;

                    case 1:
                        new Account(this.dataSource);
                        break;

                    case 2:
                        String cardNumber;
                        String PIN;

                        System.out.println("Enter your card number:");
                        cardNumber = scanner.next();

                        System.out.println("Enter your PIN:");
                        PIN = scanner.next();

                        int currAccount = -1;

                        String findStatement = "SELECT id FROM card WHERE number = ? AND pin = ?";
                        try (Connection connection = dataSource.getConnection()) {
                            try (PreparedStatement statement = connection.prepareStatement(findStatement)) {

                                statement.setString(1, cardNumber);
                                statement.setString(2, PIN);

                                ResultSet resultSet = statement.executeQuery();

                                if (resultSet.next()) {
                                    currAccount = resultSet.getInt(1);
                                    System.out.println("You have successfully logged in!");
                                }


                            }
                        } catch (SQLException e) {
                            System.out.println("Database connection error 2");
                            e.printStackTrace();
                        }

                        if (currAccount > -1) {
                            if (this.proceedLoggedActions(currAccount)) {
                                action = 0;
                            }
                        } else {
                            System.out.println("Wrong card number or PIN!");
                        }
                        break;
                    default:
                        throw new InputMismatchException("no such action");
                }
            } catch (InputMismatchException e) {
                scanner.nextLine();
                action = 1;
                System.out.println(errorMessages[0] + " Problem: " + e.getMessage());
            }
        } while (action != 0);
        System.out.println("\nBye!");
    }

    private boolean proceedLoggedActions(int accountID) {
        int action;
        do {
            System.out.println(BankingSystem.loggedMessages[0]);
            try {
                action = scanner.nextInt();
                switch (action) {

                    case 0:
                        return true;
                    case 1:
                        float balance = selectBalance(accountID);
                        if (balance >= 0) {
                            System.out.printf("Balance: %f \n", balance);
                        } else {
                            System.out.println("Error while checking balance");
                        }

                        break;
                    case 2:
                        float income;
                        System.out.println("Enter income:\n");
                        income = scanner.nextFloat();
                        if (income <= 0) {
                            System.out.println("Incorrect value");
                        } else {
                            String incomeStatement = "UPDATE card SET balance = balance + ? WHERE id = ?";
                            try (Connection connection = dataSource.getConnection()) {
                                try (PreparedStatement statement = connection.prepareStatement(incomeStatement)) {

                                    statement.setFloat(1, income);
                                    statement.setInt(2, accountID);

                                    statement.executeUpdate();

                                    System.out.println("Income was added!\n");
                                }
                            } catch (SQLException e) {
                                System.out.println("Database connection error 2");
                                e.printStackTrace();
                            }

                        }
                        break;
                    case 3:
                        String cardNumber;
                        System.out.println("Transfer\nEnter card number:");
                        cardNumber = scanner.next();
                        if (!Account.checkChecksum(cardNumber)) {
                            System.out.println("Probably you made a mistake in the card number. Please try again!");
                            break;
                        } else {
                            String ownSelectStatement = "SELECT number FROM card WHERE id = ?";
                            String destinationSelectStatement = "SELECT * FROM card WHERE number = ?";
                            try (Connection connection = dataSource.getConnection()) {
                                try (PreparedStatement statement = connection.prepareStatement(ownSelectStatement)) {

                                    statement.setInt(1, accountID);

                                    ResultSet resultSet = statement.executeQuery();

                                    resultSet.next();

                                    if (resultSet.getString(1).equals(cardNumber)) {
                                        System.out.println("You can't transfer money to the same account!");
                                        break;
                                    }
                                }

                                try (PreparedStatement statement = connection.prepareStatement(destinationSelectStatement)) {

                                    statement.setString(1, cardNumber);

                                    ResultSet resultSet = statement.executeQuery();

                                    if (!resultSet.next()) {
                                        System.out.println("Such a card does not exist.");
                                        break;
                                    }
                                }

                            } catch (SQLException e) {
                                System.out.println("Database connection error 2");
                                e.printStackTrace();
                            }
                        }

                        float transfer;
                        System.out.println("Enter how much money you want to transfer:");
                        transfer = scanner.nextFloat();

                        if (transfer > selectBalance(accountID)) {
                            System.out.println("Not enough money!");
                        } else {
                            String minusTransferStatement = "UPDATE card SET balance = balance - ? WHERE id = ?";
                            String plusTransferStatement = "UPDATE card SET balance = balance + ? WHERE number = ?";
                            try (Connection connection = dataSource.getConnection()) {
                                connection.setAutoCommit(false);
                                try (PreparedStatement minusStatement = connection.prepareStatement(minusTransferStatement);
                                     PreparedStatement plusStatement = connection.prepareStatement(plusTransferStatement)) {

                                    minusStatement.setFloat(1, transfer);
                                    minusStatement.setInt(2, accountID);
                                    plusStatement.setFloat(1, transfer);
                                    plusStatement.setString(2, cardNumber);

                                    minusStatement.executeUpdate();
                                    plusStatement.executeUpdate();

                                    connection.setAutoCommit(true);

                                    System.out.println("Success!");
                                }

                            } catch (SQLException e) {
                                System.out.println("Database connection error 2");
                                e.printStackTrace();
                            }
                        }

                        break;
                    case 4:
                        String deleteStatement = "DELETE FROM card WHERE id = ?";
                        try (Connection connection = dataSource.getConnection()) {
                            try (PreparedStatement statement = connection.prepareStatement(deleteStatement)) {

                                statement.setInt(1, accountID);

                                statement.executeUpdate();

                                System.out.println("The account has been closed!\n");
                                return false;
                            }
                        } catch (SQLException e) {
                            System.out.println("Database connection error 2");
                            e.printStackTrace();
                        }
                        break;
                    case 5:
                        System.out.println("You have successfully logged out!");
                        return false;
                    default:
                        throw new InputMismatchException("no such action");
                }
            } catch (InputMismatchException e) {
                scanner.nextLine();
                System.out.println(errorMessages[0] + " Problem: " + e.getMessage());
            }
        } while (true);
    }

    private void createDatabase(String dbName) {
        String url = "jdbc:sqlite:./" + dbName;

        this.dataSource.setUrl(url);

        //System.out.println(this.dataSource.getUrl());

        try (Connection connection = dataSource.getConnection()) {

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS card(" +
                        "id INTEGER PRIMARY KEY," +
                        "number VARCHAR(16) UNIQUE," +
                        "pin VARCHAR(4)," +
                        "balance INTEGER DEFAULT 0)");

            }

        } catch (SQLException e) {
            System.out.println("Database connection error");
            e.printStackTrace();
        }
    }

    private String getDBName(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-fileName")) {
                    return args[i+1];
                }
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return "card.s3db";
        }
        return "card.s3db";
    }

    private float selectBalance(int id) {
        String balanceStatement = "SELECT balance FROM card WHERE id = ?";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(balanceStatement)) {

                statement.setInt(1, id);

                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return resultSet.getFloat(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database connection error 2");
            e.printStackTrace();
        }
        return -1;
    }

}
