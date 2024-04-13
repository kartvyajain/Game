import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class GameManagementSystem extends JFrame {
    private JButton selectButton, exitButton, statsButton;
    private JList<String> gameList;
    private JTable gameStatsTable;
    private DefaultTableModel tableModel;
    private Connection connection;
    private String phoneNumber;

    public GameManagementSystem() {
        connectToDatabase();
        getUserInfo();
    }

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/game", "root", "Kartavyajain2004");
            System.out.println("Connected to the database.");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
        }
    }

    private void getUserInfo() {
        boolean isValidInput = false;
        do {
            JTextField nameField = new JTextField(10);
            JTextField ageField = new JTextField(10);
            JTextField phoneField = new JTextField(10);
            JPanel userInfoPanel = new JPanel();
            userInfoPanel.setLayout(new GridLayout(3, 2));
            userInfoPanel.add(new JLabel("Name:"));
            userInfoPanel.add(nameField);
            userInfoPanel.add(new JLabel("Age:"));
            userInfoPanel.add(ageField);
            userInfoPanel.add(new JLabel("Phone Number:"));
            userInfoPanel.add(phoneField);

            int result = JOptionPane.showConfirmDialog(null, userInfoPanel, "Enter Your Information",
                    JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String name = nameField.getText();
                String ageText = ageField.getText();
                phoneNumber = phoneField.getText();
                try {
                    int age = Integer.parseInt(ageText);
                    if (age < 18) {
                        JOptionPane.showMessageDialog(null, "You must be 18 or older to play games.");
                    } else if (phoneNumber.length() != 10 || !phoneNumber.matches("\\d+")) {
                        JOptionPane.showMessageDialog(null, "Please enter a valid 10-digit phone number.");
                    } else {
                        isValidInput = true;
                        insertUserData(name, age, phoneNumber);
                        initializeUI();
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "Please enter a valid age.");
                }
            } else {
                System.exit(0);
            }
        } while (!isValidInput);
    }

    private void insertUserData(String name, int age, String phoneNumber) {
        try {
            String insertUserSQL = "INSERT INTO users (phone, name, age) VALUES (?, ?, ?)";
            PreparedStatement userStatement = connection.prepareStatement(insertUserSQL);
            userStatement.setString(1, phoneNumber);
            userStatement.setString(2, name);
            userStatement.setInt(3, age);
            userStatement.executeUpdate();

            String insertStatsSQL = "INSERT INTO stats (phone) VALUES (?)";
            PreparedStatement statsStatement = connection.prepareStatement(insertStatsSQL);
            statsStatement.setString(1, phoneNumber);
            statsStatement.executeUpdate();

            System.out.println("User data inserted successfully.");
        } catch (SQLException e) {
            System.err.println("Error inserting user data: " + e.getMessage());
        }
    }

    private void initializeUI() {
        setTitle("Game Management System");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String[] games = { "Guess Number", "Toss" };
        gameList = new JList<>(games);
        JScrollPane gameListScrollPane = new JScrollPane(gameList);
        JLabel gameListLabel = new JLabel("Select a Game:");
        selectButton = new JButton("Select");
        exitButton = new JButton("Exit");
        statsButton = new JButton("Game Statistics");

        tableModel = new DefaultTableModel();
        tableModel.addColumn("Game");
        tableModel.addColumn("Games Won");
        tableModel.addColumn("Games Lost");
        tableModel.addRow(new Object[] { "Guess Number", 0, 0 });
        tableModel.addRow(new Object[] { "Toss", 0, 0 });
        gameStatsTable = new JTable(tableModel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(selectButton);
        buttonPanel.add(exitButton);
        buttonPanel.add(statsButton);
        mainPanel.add(gameListLabel, BorderLayout.NORTH);
        mainPanel.add(gameListScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);

        selectButton.addActionListener(e -> launchGame(gameList.getSelectedValue()));
        exitButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(null, "Thank you for playing!");
            System.exit(0);
        });
        statsButton.addActionListener(e -> displayStats(phoneNumber));

        setVisible(true);
    }

    private void displayStats(String phoneNumber) {
        try {
            String selectStatsSQL = "SELECT * FROM stats WHERE phone = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(selectStatsSQL);
            preparedStatement.setString(1, phoneNumber);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int gamesWonGuessNumber = resultSet.getInt("games_won_guess_number");
                int gamesLostGuessNumber = resultSet.getInt("games_lost_guess_number");
                int gamesWonToss = resultSet.getInt("games_won_toss");
                int gamesLostToss = resultSet.getInt("games_lost_toss");

                String[][] statsData = {
                        { "Guess Number", String.valueOf(gamesWonGuessNumber), String.valueOf(gamesLostGuessNumber) },
                        { "Toss", String.valueOf(gamesWonToss), String.valueOf(gamesLostToss) }
                };

                String[] columnNames = { "Game", "Games Won", "Games Lost" };
                JTable userStatsTable = new JTable(statsData, columnNames);

                JOptionPane.showMessageDialog(null, new JScrollPane(userStatsTable),
                        "Game Statistics for " + phoneNumber, JOptionPane.PLAIN_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "No statistics found for the user.", "Game Statistics",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user statistics: " + e.getMessage());
        }
    }

    private void updateGameStats(String gameName, boolean won, String phoneNumber) {
        try {
            String columnPrefix;
            if (won) {
                columnPrefix = "games_won_";
            } else {
                columnPrefix = "games_lost_";
            }
    
            // Correctly format the gameName to match the column name in the database
            String columnName = gameName.toLowerCase().replace(" ", "_");
    
            // Construct the SQL query
            String updateStatsSQL = "UPDATE stats SET " + columnPrefix + columnName
                    + " = " + columnPrefix + columnName + " + 1 WHERE phone = ?";
    
            // Prepare and execute the statement
            PreparedStatement preparedStatement = connection.prepareStatement(updateStatsSQL);
            preparedStatement.setString(1, phoneNumber);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating game statistics: " + e.getMessage());
        }
    }
        
    
    
    private void launchGame(String gameName) {
        switch (gameName) {
            case "Guess Number":
                playGuessNumber();
                break;
            case "Toss":
                playToss();
                break;
        }
    }

    private void playGuessNumber() {
        int userGuess = 0;
        boolean isValidGuess = false;

        while (!isValidGuess) {
            String userInput = JOptionPane.showInputDialog(null, "Guess Number between 1 and 50:");
            if (userInput != null) {
                try {
                    userGuess = Integer.parseInt(userInput);
                    isValidGuess = userGuess >= 1 && userGuess <= 50;
                    if (!isValidGuess)
                        JOptionPane.showMessageDialog(null, "Please enter a number between 1 and 50.");
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "Please enter a valid number.");
                }
            } else
                return;
        }

        int randomNumber = (int) (Math.random() * 50) + 1;
        boolean won = userGuess == randomNumber;
        JOptionPane.showMessageDialog(null, won ? "Congratulations! You win! The number was: " + randomNumber
                : "Sorry, you guessed wrong. The number was: " + randomNumber);
        updateGameStats("Guess Number", won, phoneNumber);

    }

    private void playToss() {
        String[] coinSides = { "Heads", "Tails" };
        int randomNumber = (int) (Math.random() * 2);
        JRadioButton headsRadioButton = new JRadioButton("Heads");
        JRadioButton tailsRadioButton = new JRadioButton("Tails");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(headsRadioButton);
        buttonGroup.add(tailsRadioButton);
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
        radioPanel.add(headsRadioButton);
        radioPanel.add(tailsRadioButton);

        int result = JOptionPane.showConfirmDialog(null, radioPanel, "Make a guess:", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int userGuess = headsRadioButton.isSelected() ? 1 : 2;
            String resultMessage = "The coin shows: " + coinSides[randomNumber] + " and you selected "
                    + (userGuess == 1 ? "heads" : "tails") + "\n";
            boolean won = (randomNumber == 0 && userGuess == 1) || (randomNumber == 1 && userGuess == 2);
            resultMessage += won ? "Congratulations! You won !" : "Sorry, you didn't win this time.";
            JOptionPane.showMessageDialog(null, resultMessage);
            updateGameStats("Toss", won, phoneNumber);

        }
    }

    public static void main(String[] args) {
        new GameManagementSystem();
    }
}
// import javax.swing.*;
// import javax.swing.table.DefaultTableModel;
// import java.awt.*;
// import java.sql.*;

// public class GameManagementSystem extends JFrame {
//     private JButton selectButton, exitButton, statsButton;
//     private JList<String> gameList;
//     private JTable gameStatsTable;
//     private DefaultTableModel tableModel;
//     private Connection connection;
//     private String phoneNumber;

//     public GameManagementSystem() {
//         connectToDatabase();
//         getUserInfo();
//     }

//     private void connectToDatabase() {
//         try {
//             Class.forName("com.mysql.jdbc.Driver");
//             connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/game", "root", "Kartavyajain2004");
//             System.out.println("Connected to the database.");
//         } catch (ClassNotFoundException | SQLException e) {
//             System.err.println("Error connecting to the database: " + e.getMessage());
//         }
//     }

//     private void getUserInfo() {
//         boolean isValidInput = false;
//         do {
//             JTextField nameField = new JTextField(10);
//             JTextField ageField = new JTextField(10);
//             JTextField phoneField = new JTextField(10);
//             JPanel userInfoPanel = new JPanel();
//             userInfoPanel.setLayout(new GridLayout(3, 2));
//             userInfoPanel.add(new JLabel("Name:"));
//             userInfoPanel.add(nameField);
//             userInfoPanel.add(new JLabel("Age:"));
//             userInfoPanel.add(ageField);
//             userInfoPanel.add(new JLabel("Phone Number:"));
//             userInfoPanel.add(phoneField);

//             int result = JOptionPane.showConfirmDialog(null, userInfoPanel, "Enter Your Information",
//                     JOptionPane.OK_CANCEL_OPTION);
//             if (result == JOptionPane.OK_OPTION) {
//                 String name = nameField.getText();
//                 String ageText = ageField.getText();
//                 phoneNumber = phoneField.getText();
//                 try {
//                     int age = Integer.parseInt(ageText);
//                     if (age < 18) {
//                         JOptionPane.showMessageDialog(null, "You must be 18 or older to play games.");
//                     } else if (phoneNumber.length() != 10 || !phoneNumber.matches("\\d+")) {
//                         JOptionPane.showMessageDialog(null, "Please enter a valid 10-digit phone number.");
//                     } else {
//                         isValidInput = true;
//                         insertUserData(name, age, phoneNumber);
//                         initializeUI();
//                     }
//                 } catch (NumberFormatException e) {
//                     JOptionPane.showMessageDialog(null, "Please enter a valid age.");
//                 }
//             } else {
//                 System.exit(0);
//             }
//         } while (!isValidInput);
//     }

//     private void insertUserData(String name, int age, String phoneNumber) {
//         try {
//             String insertUserSQL = "INSERT INTO users (phone, name, age) VALUES (?, ?, ?)";
//             PreparedStatement userStatement = connection.prepareStatement(insertUserSQL);
//             userStatement.setString(1, phoneNumber);
//             userStatement.setString(2, name);
//             userStatement.setInt(3, age);
//             userStatement.executeUpdate();

//             String insertStatsSQL = "INSERT INTO stats (phone) VALUES (?)";
//             PreparedStatement statsStatement = connection.prepareStatement(insertStatsSQL);
//             statsStatement.setString(1, phoneNumber);
//             statsStatement.executeUpdate();

//             System.out.println("User data inserted successfully.");
//         } catch (SQLException e) {
//             System.err.println("Error inserting user data: " + e.getMessage());
//         }
//     }

//     private void initializeUI() {
//         setTitle("Game Management System");
//         setSize(600, 400);
//         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//         String[] games = { "Guess Number", "Toss", "Binary Search" };
//         gameList = new JList<>(games);
//         JScrollPane gameListScrollPane = new JScrollPane(gameList);
//         JLabel gameListLabel = new JLabel("Select a Game:");
//         selectButton = new JButton("Select");
//         exitButton = new JButton("Exit");
//         statsButton = new JButton("Game Statistics");

//         tableModel = new DefaultTableModel();
//         tableModel.addColumn("Game");
//         tableModel.addColumn("Games Won");
//         tableModel.addColumn("Games Lost");
//         tableModel.addRow(new Object[] { "Guess Number", 0, 0 });
//         tableModel.addRow(new Object[] { "Toss", 0, 0 });
//         tableModel.addRow(new Object[] { "Binary Search", 0, 0 });
//         gameStatsTable = new JTable(tableModel);

//         JPanel mainPanel = new JPanel(new BorderLayout());
//         JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//         buttonPanel.add(selectButton);
//         buttonPanel.add(exitButton);
//         buttonPanel.add(statsButton);
//         mainPanel.add(gameListLabel, BorderLayout.NORTH);
//         mainPanel.add(gameListScrollPane, BorderLayout.CENTER);
//         mainPanel.add(buttonPanel, BorderLayout.SOUTH);
//         add(mainPanel);

//         selectButton.addActionListener(e -> launchGame(gameList.getSelectedValue()));
//         exitButton.addActionListener(e -> {
//             JOptionPane.showMessageDialog(null, "Thank you for playing!");
//             System.exit(0);
//         });
//         statsButton.addActionListener(e -> displayStats(phoneNumber));

//         setVisible(true);
//     }

//     private void displayStats(String phoneNumber) {
//         try {
//             String selectStatsSQL = "SELECT * FROM stats WHERE phone = ?";
//             PreparedStatement preparedStatement = connection.prepareStatement(selectStatsSQL);
//             preparedStatement.setString(1, phoneNumber);
//             ResultSet resultSet = preparedStatement.executeQuery();

//             if (resultSet.next()) {
//                 int gamesWonGuessNumber = resultSet.getInt("games_won_guess_number");
//                 int gamesLostGuessNumber = resultSet.getInt("games_lost_guess_number");
//                 int gamesWonToss = resultSet.getInt("games_won_toss");
//                 int gamesLostToss = resultSet.getInt("games_lost_toss");
//                 int gamesWonBinarySearch = resultSet.getInt("games_won_binary_search");
//                 int gamesLostBinarySearch = resultSet.getInt("games_lost_binary_search");

//                 String[][] statsData = {
//                         { "Guess Number", String.valueOf(gamesWonGuessNumber), String.valueOf(gamesLostGuessNumber) },
//                         { "Toss", String.valueOf(gamesWonToss), String.valueOf(gamesLostToss) },
//                         { "Binary Search", String.valueOf(gamesWonBinarySearch), String.valueOf(gamesLostBinarySearch) }
//                 };

//                 String[] columnNames = { "Game", "Games Won", "Games Lost" };
//                 JTable userStatsTable = new JTable(statsData, columnNames);

//                 JOptionPane.showMessageDialog(null, new JScrollPane(userStatsTable),
//                         "Game Statistics for " + phoneNumber, JOptionPane.PLAIN_MESSAGE);
//             } else {
//                 JOptionPane.showMessageDialog(null, "No statistics found for the user.", "Game Statistics",
//                         JOptionPane.INFORMATION_MESSAGE);
//             }
//         } catch (SQLException e) {
//             System.err.println("Error retrieving user statistics: " + e.getMessage());
//         }
//     }

//     private void updateGameStats(String gameName, boolean won, String phoneNumber) {
//         try {
//             String columnPrefix;
//             if (won) {
//                 columnPrefix = "games_won_";
//             } else {
//                 columnPrefix = "games_lost_";
//             }

//             // Correctly format the gameName to match the column name in the database
//             String columnName = gameName.toLowerCase().replace(" ", "_");

//             // Construct the SQL query
//             String updateStatsSQL = "UPDATE stats SET " + columnPrefix + columnName
//                     + " = " + columnPrefix + columnName + " + 1 WHERE phone = ?";

//             // Prepare and execute the statement
//             PreparedStatement preparedStatement = connection.prepareStatement(updateStatsSQL);
//             preparedStatement.setString(1, phoneNumber);
//             preparedStatement.executeUpdate();
//         } catch (SQLException e) {
//             System.err.println("Error updating game statistics: " + e.getMessage());
//         }
//     }

//     private void launchGame(String gameName) {
//         switch (gameName) {
//             case "Guess Number":
//                 playGuessNumber();
//                 break;
//             case "Toss":
//                 playToss();
//                 break;
//             case "Binary Search":
//                 playBinarySearch();
//                 break;
//         }
//     }

//     private void playGuessNumber() {
//         int userGuess = 0;
//         boolean isValidGuess = false;

//         while (!isValidGuess) {
//             String userInput = JOptionPane.showInputDialog(null, "Guess Number between 1 and 50:");
//             if (userInput != null) {
//                 try {
//                     userGuess = Integer.parseInt(userInput);
//                     isValidGuess = userGuess >= 1 && userGuess <= 50;
//                     if (!isValidGuess)
//                         JOptionPane.showMessageDialog(null, "Please enter a number between 1 and 50.");
//                 } catch (NumberFormatException e) {
//                     JOptionPane.showMessageDialog(null, "Please enter a valid number.");
//                 }
//             } else
//                 return;
//         }

//         int randomNumber = (int) (Math.random() * 50) + 1;
//         boolean won = userGuess == randomNumber;
//         JOptionPane.showMessageDialog(null, won ? "Congratulations! You win! The number was: " + randomNumber
//                 : "Sorry, you guessed wrong. The number was: " + randomNumber);
//         updateGameStats("Guess Number", won, phoneNumber);

//     }

//     private void playToss() {
//         String[] coinSides = { "Heads", "Tails" };
//         int randomNumber = (int) (Math.random() * 2);
//         JRadioButton headsRadioButton = new JRadioButton("Heads");
//         JRadioButton tailsRadioButton = new JRadioButton("Tails");
//         ButtonGroup buttonGroup = new ButtonGroup();
//         buttonGroup.add(headsRadioButton);
//         buttonGroup.add(tailsRadioButton);
//         JPanel radioPanel = new JPanel();
//         radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
//         radioPanel.add(headsRadioButton);
//         radioPanel.add(tailsRadioButton);

//         int result = JOptionPane.showConfirmDialog(null, radioPanel, "Make a guess:", JOptionPane.OK_CANCEL_OPTION);
//         if (result == JOptionPane.OK_OPTION) {
//             int userGuess = headsRadioButton.isSelected() ? 1 : 2;
//             String resultMessage = "The coin shows: " + coinSides[randomNumber] + " and you selected "
//                     + (userGuess == 1 ? "heads" : "tails") + "\n";
//             boolean won = (randomNumber == 0 && userGuess == 1) || (randomNumber == 1 && userGuess == 2);
//             resultMessage += won ? "Congratulations! You won !" : "Sorry, you didn't win this time.";
//             JOptionPane.showMessageDialog(null, resultMessage);
//             updateGameStats("Toss", won, phoneNumber);

//         }
//     }

//     private void playBinarySearch() {
//         int targetNumber = (int) (Math.random() * 10) + 1;
//         int lowerBound = 1;
//         int upperBound = 10;
//         int numberOfAttempts = 0;

//         while (true) {
//             numberOfAttempts++;
//             String input = JOptionPane.showInputDialog(null,
//                     "Guess the number between " + lowerBound + " and " + upperBound);
//             if (input == null) // User canceled the input dialog
//                 return;

//             try {
//                 int guess = Integer.parseInt(input);
//                 if(guess>upperBound)
//                 JOptionPane.showMessageDialog(null, "Try a lower number.");
//                else if (guess < targetNumber) {
//                     lowerBound = guess + 1;
//                     JOptionPane.showMessageDialog(null, "Try a higher number.");
//                 } else if (guess > targetNumber) {
//                     upperBound = guess - 1;
//                     JOptionPane.showMessageDialog(null, "Try a lower number.");
//                 } else {
//                     JOptionPane.showMessageDialog(null, "Congratulations! You guessed the number in "
//                             + numberOfAttempts + " attempts.");
//                     updateGameStats("Binary Search", true, phoneNumber);
//                     return;
//                 }
//             } catch (NumberFormatException e) {
//                 JOptionPane.showMessageDialog(null, "Please enter a valid number.");
//             }

//             if (lowerBound > upperBound) {
//                 JOptionPane.showMessageDialog(null, "Sorry, you ran out of attempts. The number was: " + targetNumber);
//                 updateGameStats("Binary Search", false, phoneNumber);
//                 return;
//             }
//         }
//     }

//     public static void main(String[] args) {
//         new GameManagementSystem();
//     }
// }
