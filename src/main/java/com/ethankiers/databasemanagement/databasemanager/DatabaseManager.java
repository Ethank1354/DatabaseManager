package com.ethankiers.databasemanagement.databasemanager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

public class DatabaseManager {

    private static final Logger log = LogManager.getLogger(DatabaseManager.class);
    private Connection conn;

    public DatabaseManager(String filePath) {
        String url = "jdbc:sqlite:" + filePath;
        try {
            conn = DriverManager.getConnection(url);
            System.out.println("Connected to database");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }

    public boolean importXlsxToDatabase(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            // Iterate over all sheets in the workbook
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String tableName = sheet.getSheetName().replace(" ", ""); // Replace spaces in table names
                System.out.println("Processing sheet: " + tableName);

                Iterator<Row> rowIterator = sheet.iterator();
                if (!rowIterator.hasNext()) {
                    System.out.println("Skipping empty sheet: " + tableName);
                    continue; // Skip empty sheets
                }

                // Read column headers
                Row headerRow = rowIterator.next();
                int columnCount = headerRow.getPhysicalNumberOfCells();
                List<String> columnNames = new ArrayList<>();

                for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                    String columnName = headerRow.getCell(colIndex).getStringCellValue().trim();

                    // Handle spaces by quoting column names properly
                    if (columnName.contains(" ")) {
                        columnName = "\"" + columnName + "\"";
                    }
                    columnNames.add(columnName);
                }

                // Create table
                String createTableSQL = "CREATE TABLE IF NOT EXISTS \"" + tableName + "\" (" +
                        String.join(" TEXT, ", columnNames) + " TEXT);";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTableSQL);
                }

                // Prepare INSERT query with named columns
                String insertSQL = "INSERT INTO \"" + tableName + "\" (" +
                        String.join(", ", columnNames) + ") VALUES (" +
                        String.join(", ", Collections.nCopies(columnCount, "?")) + ");";

                try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                    // Insert rows
                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                            Cell cell = row.getCell(colIndex);
                            if (cell != null) {
                                pstmt.setString(colIndex + 1, cell.toString());
                            } else {
                                pstmt.setNull(colIndex + 1, Types.NULL);
                            }
                        }
                        pstmt.executeUpdate();
                    }
                }
            }

            return true;
        } catch (IOException | SQLException e) {
            System.out.println("Error importing data: " + e.getMessage());
            return false;
        }
    }


    public boolean addRowToTable(String tableName, String[] values) throws SQLException {
        // Build a parameterized SQL query
        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " VALUES(");

        // Add placeholders for each value
        for (int i = 0; i < values.length; i++) {
            sql.append("?");
            if (i < values.length - 1) {
                sql.append(",");
            }
        }
        sql.append(");");

        // Prepare the statement and set values
        try (PreparedStatement pst = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.length; i++) {
                pst.setString(i + 1, values[i]);
            }

            int rowsAffected = pst.executeUpdate();
            return rowsAffected > 0; // Return true if row was added successfully
        } catch (SQLException e) {
            System.out.println("Error inserting row: " + e.getMessage());
            return false; // Return false if there was an error
        }
    }


    public boolean updateRowInTable(String tableName, String columnName, String columnValue, String[] newValues) throws SQLException {
        // Build the SET clause of the SQL query dynamically based on the number of columns
        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
        for (int i = 0; i < newValues.length; i++) {
            sql.append("column" + (i + 1) + " = ?");
            if (i < newValues.length - 1) {
                sql.append(", ");
            }
        }
        sql.append(" WHERE " + columnName + " = ?;");

        // Prepare the statement and set the values
        try (PreparedStatement pst = conn.prepareStatement(sql.toString())) {
            // Set the new values for the row
            for (int i = 0; i < newValues.length; i++) {
                pst.setString(i + 1, newValues[i]);
            }
            // Set the condition for the row update
            pst.setString(newValues.length + 1, columnValue);

            int rowsAffected = pst.executeUpdate();
            return rowsAffected > 0; // Return true if at least one row was updated
        } catch (SQLException e) {
            System.out.println("Error updating row: " + e.getMessage());
            return false; // Return false if there was an error
        }
    }

    public String[] returnStringArray(String tableName, String columnName, String field) throws SQLException {
        String sql = "SELECT * FROM " + tableName + " WHERE " + columnName + " = ?;";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, field);
            ResultSet result = pst.executeQuery();

            // Store the results in a list
            List<String> rowData = new ArrayList<>();
            ResultSetMetaData metaData = result.getMetaData();
            int columnCount = metaData.getColumnCount();

            if (result.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    rowData.add(result.getString(i));
                }
            }

            return rowData.toArray(new String[0]); // Convert list to array
        }
    }

    public List<String[]> getFilteredValues(String tableName, String[] columns, String filterColumn, String filterValue) throws SQLException {
        // Build the SELECT clause of the SQL query dynamically based on the columns array
        StringBuilder sql = new StringBuilder("SELECT ");
        for (int i = 0; i < columns.length; i++) {
            sql.append(columns[i]);
            if (i < columns.length - 1) {
                sql.append(", ");
            }
        }
        sql.append(" FROM " + tableName + " WHERE " + filterColumn + " = ?;");

        List<String[]> results = new ArrayList<>();

        try (PreparedStatement pst = conn.prepareStatement(sql.toString())) {
            // Set the filter value in the query
            pst.setString(1, filterValue);

            // Execute the query and process the result set
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    String[] row = new String[columns.length];
                    for (int i = 0; i < columns.length; i++) {
                        row[i] = resultSet.getString(columns[i]);
                    }
                    results.add(row); // Add the row to the results list
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving filtered values: " + e.getMessage());
        }

        return results;
    }

    public boolean deleteRowFromTable(String tableName, String filterColumn, String filterValue) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + filterColumn + " = ?;";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            // Set the filter value in the query
            pst.setString(1, filterValue);

            int rowsAffected = pst.executeUpdate();
            return rowsAffected > 0; // Return true if a row was deleted
        } catch (SQLException e) {
            System.out.println("Error deleting row: " + e.getMessage());
            return false; // Return false if there was an error
        }
    }

    public boolean belongsToTable(String tableName, String field) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet result = pst.executeQuery()) {

            while (result.next()) {
                String columnName = result.getString("name");
                if (columnName.equalsIgnoreCase(field)) {
                    return true; // Column exists
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking table structure: " + e.getMessage());
        }

        return false; // Column does not exist
    }

    public List<String> getTableHeaders(String tableName) throws SQLException {
        Statement st = conn.createStatement();
        List<String> headers = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " LIMIT 1"; // Fetch only one row for efficiency

        try (ResultSet rs = st.executeQuery(query)) {
            if (rs != null) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    headers.add(metaData.getColumnName(i));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving table headers: " + e.getMessage());
        }

        return headers;
    }

    public List<String> getColumnValues(String tableName, String columnName) throws SQLException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT " + columnName + " FROM " + tableName;

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet result = pst.executeQuery()) {

            while (result.next()) {
                values.add(result.getString(1)); // Get the first (and only) column value
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving column values: " + e.getMessage());
        }

        return values;
    }

    //working
    public List<String> getTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';"; // Exclude internal SQLite tables

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                tables.add(rs.getString("name"));
            }
        }
        return tables;
    }

    public static void main(String args[]) throws SQLException {
        DatabaseManager db = new DatabaseManager("/home/user/test.db");
        db.importXlsxToDatabase("/home/user/QubesIncoming/school/UMS_Data.xlsx");
        List<String> tables = db.getTables();

        System.out.println("\nTables");
        for (String header : tables) {
            System.out.println(header);
        }

        List<String> headers = db.getTableHeaders(tables.get(0));

        System.out.println("\nHeaders");
        for (String header : headers) {
            System.out.println(header);
        }

        //db.importXlsxToDatabase("/home/user/QubesIncoming/school/UMS_Data.xlsx");

        //System.out.println("\nColumn Values");
        //List<String> values = db.getColumnValues(tables.get(0), headers.get(0));
    }
}
