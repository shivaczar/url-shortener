package org.example.util;

import java.sql.*;
import java.util.Random;
import java.util.UUID;

public class URLShortenerDatabase {
    private static final String DB_URL = "jdbc:sqlite:database.db";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("✅ Connected to SQLite database.");
                createTable(conn);
                insertDummyData(conn);
            }
        } catch (SQLException e) {
            System.out.println("❌ Database connection failed: " + e.getMessage());
        }
    }

    private static void createTable(Connection conn) {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS url_shortener (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                original_url TEXT NOT NULL,
                short_code TEXT UNIQUE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("✅ Table 'url_shortener' is ready.");
        } catch (SQLException e) {
            System.out.println("❌ Table creation failed: " + e.getMessage());
        }
    }

    private static void insertDummyData(Connection conn) {
        String insertSQL = "INSERT INTO url_shortener (original_url, short_code) VALUES (?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            for (int i = 1; i <= 1000; i++) {
                pstmt.setString(1, generateRandomURL());
                pstmt.setString(2, generateShortCode());
                pstmt.addBatch(); // Add to batch for efficiency

                if (i % 100 == 0) {
                    pstmt.executeBatch(); // Execute batch every 100 rows
                    System.out.println("Inserted " + i + " rows...");
                }
            }



            pstmt.executeBatch(); // Insert remaining records
            System.out.println("✅ 1000 URLs inserted successfully!");
        } catch (SQLException e) {
            System.out.println("❌ Data insertion failed: " + e.getMessage());
        }
    }

    private static String generateRandomURL() {
        String[] domains = {"google.com", "youtube.com", "facebook.com", "twitter.com", "github.com"};
        return "https://www." + domains[new Random().nextInt(domains.length)] + "/" + UUID.randomUUID();
    }

    private static String generateShortCode() {
        return UUID.randomUUID().toString().substring(0, 6); // 6-character unique short code
    }
}

