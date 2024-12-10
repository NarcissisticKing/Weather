import java.sql.*;
import java.security.MessageDigest;
import java.util.Scanner;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class WeatherApp {

    // Database initialization
    public static void initDb() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:weather_app.db")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL);");
            stmt.execute("CREATE TABLE IF NOT EXISTS search_history (id INTEGER PRIMARY KEY AUTOINCREMENT, city TEXT NOT NULL, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, user_id INTEGER, FOREIGN KEY (user_id) REFERENCES users(id));");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    // Function to register a new user
    public static void registerUser() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter username: ");
        String username = sc.nextLine();
        System.out.print("Enter password: ");
        String password = sc.nextLine();
        String hashedPassword = hashPassword(password);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:weather_app.db")) {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
            System.out.println("Registration successful!");
        } catch (SQLException e) {
            System.err.println("Error during registration: " + e.getMessage());
        }
    }

    // Function to log in a user
    public static int loginUser() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter username: ");
        String username = sc.nextLine();
        System.out.print("Enter password: ");
        String password = sc.nextLine();
        String hashedPassword = hashPassword(password);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:weather_app.db")) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ? AND password = ?");
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            } else {
                System.out.println("Invalid login or password.");
                return -1;
            }
        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
            return -1;
        }
    }

    // Hash the password using SHA-256
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            System.err.println("Error hashing password: " + e.getMessage());
            return null;
        }
    }

    // Fetch current weather information from OpenWeatherMap API
    public static String getWeather(String city) {
        String apiKey = "YOUR_OPENWEATHERMAP_API_KEY";  // Replace with your OpenWeatherMap API key
        String urlString = "http://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey + "&units=metric";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } catch (Exception e) {
            System.err.println("Error fetching weather data: " + e.getMessage());
            return null;
        }
    }

    // Save search history to the database
    public static void saveSearchHistory(int userId, String city) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:weather_app.db")) {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO search_history (city, user_id) VALUES (?, ?)");
            pstmt.setString(1, city);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving search history: " + e.getMessage());
        }
    }

    // Show the user's search history
    public static void showSearchHistory(int userId) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:weather_app.db")) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM search_history WHERE user_id = ?");
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                System.out.println("No search history found.");
            } else {
                while (rs.next()) {
                    System.out.println("City: " + rs.getString("city") + ", Time: " + rs.getString("timestamp"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching search history: " + e.getMessage());
        }
    }

    // Admin interface to view all search history
    public static void adminInterface() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter admin password: ");
        String adminPassword = sc.nextLine();

        if (adminPassword.equals("admin123")) {  // Admin password (hardcoded)
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:weather_app.db")) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT users.username, search_history.city, search_history.timestamp FROM search_history JOIN users ON search_history.user_id = users.id");

                if (!rs.isBeforeFirst()) {
                    System.out.println("No search history logs found.");
                } else {
                    while (rs.next()) {
                        System.out.println("User: " + rs.getString("username") + ", City: " + rs.getString("city") + ", Time: " + rs.getString("timestamp"));
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error fetching search logs: " + e.getMessage());
            }
        } else {
            System.out.println("Incorrect admin password.");
        }
    }

    public static void main(String[] args) {
        initDb();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n1. Login");
            System.out.println("2. Register");
            System.out.println("3. Admin Interface");
            System.out.println("4. Exit");
            System.out.print("Select an option: ");
            int choice = sc.nextInt();
            sc.nextLine();  // consume the newline

            if (choice == 1) {
                int userId = loginUser();
                if (userId != -1) {
                    while (true) {
                        System.out.println("\n1. Get Weather");
                        System.out.println("2. View Search History");
                        System.out.println("3. Logout");
                        System.out.print("Select an option: ");
                        int action = sc.nextInt();
                        sc.nextLine();  // consume the newline

                        if (action == 1) {
                            System.out.print("Enter city: ");
                            String city = sc.nextLine();
                            String weatherData = getWeather(city);
                            if (weatherData != null) {
                                System.out.println("Weather data: " + weatherData);
                                saveSearchHistory(userId, city);
                            }
                        } else if (action == 2) {
                            showSearchHistory(userId);
                        } else if (action == 3) {
                            break;
                        } else {
                            System.out.println("Invalid option.");
                        }
                    }
                }
            } else if (choice == 2) {
                registerUser();
            } else if (choice == 3) {
                adminInterface();
            } else if (choice == 4) {
                break;
            } else {
                System.out.println("Invalid option.");
            }
        }
    }
}
