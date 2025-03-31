package com.ethankiers.databasemanagement.databasemanager;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.util.List;
import java.util.Scanner;

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
    //working
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

    //working
    public boolean addRowToTable(String tableName, String[] values) throws SQLException {
        // Quote the table name if it contains spaces
        String quotedTableName = tableName.contains(" ") ? "\"" + tableName + "\"" : tableName;

        // Build a parameterized SQL query
        StringBuilder sql = new StringBuilder("INSERT INTO " + quotedTableName + " VALUES(");

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


    //working
    public boolean updateRowInTable(String tableName, String columnName, String columnValue, List<String> newValues) throws SQLException {

        List<String> columnNames = this.getTableHeaders(tableName);

        // Quote table and column names if they contain spaces
        String quotedTableName = tableName.contains(" ") ? "\"" + tableName + "\"" : tableName;
        String quotedColumnName = columnName.contains(" ") ? "\"" + columnName + "\"" : columnName;

        // Check if the number of column names matches the number of new values
        if (columnNames.size() != newValues.size()) {
            throw new SQLException("Column names and new values count must be the same.");
        }

        // Build the SET clause of the SQL query dynamically based on the column names list
        StringBuilder sql = new StringBuilder("UPDATE " + quotedTableName + " SET ");
        for (int i = 0; i < columnNames.size(); i++) {
            String quotedColumn = columnNames.get(i).contains(" ") ? "\"" + columnNames.get(i) + "\"" : columnNames.get(i);
            sql.append(quotedColumn + " = ?");
            if (i < columnNames.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(" WHERE " + quotedColumnName + " = ?;");

        // Prepare the statement and set the values
        try (PreparedStatement pst = conn.prepareStatement(sql.toString())) {
            // Set the new values for the row
            for (int i = 0; i < newValues.size(); i++) {
                pst.setString(i + 1, newValues.get(i));
            }
            // Set the condition for the row update
            pst.setString(columnNames.size() + 1, columnValue);

            int rowsAffected = pst.executeUpdate();
            return rowsAffected > 0; // Return true if at least one row was updated
        } catch (SQLException e) {
            System.out.println("Error updating row: " + e.getMessage());
            return false; // Return false if there was an error
        }
    }

    //working
    public List<String> getRow(String tableName, String columnName, String field) throws SQLException {
        // Quote table and column names if they contain spaces
        String quotedTableName = tableName.contains(" ") ? "\"" + tableName + "\"" : tableName;
        String quotedColumnName = columnName.contains(" ") ? "\"" + columnName + "\"" : columnName;

        // SQL query to select the row based on the condition
        String sql = "SELECT * FROM " + quotedTableName + " WHERE " + quotedColumnName + " = ?;";

        List<String> rowData = new ArrayList<>();

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, field);
            ResultSet result = pst.executeQuery();

            // Retrieve and store the row data if it exists
            if (result.next()) {
                ResultSetMetaData metaData = result.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    rowData.add(result.getString(i)); // Add each column's value to the list
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving row: " + e.getMessage());
        }

        return rowData; // Return the list instead of an array
    }

    //works but only for multiple columns, not singular ones
    public List<String> getFilteredValues(String tableName, String[] columns, String filterColumn, String filterValue) throws SQLException {
        if (columns.length == 0) {
            throw new IllegalArgumentException("Columns array must have at least one column.");
        }

        // Quote table and column names if they contain spaces
        String quotedTableName = tableName.contains(" ") ? "\"" + tableName + "\"" : tableName;
        String quotedFilterColumn = filterColumn.contains(" ") ? "\"" + filterColumn + "\"" : filterColumn;

        // Quote each column name properly
        String[] quotedColumns = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            quotedColumns[i] = columns[i].contains(" ") ? "\"" + columns[i] + "\"" : columns[i];
        }

        // Build the SELECT query
        String sql = "SELECT " + String.join(", ", quotedColumns) + " FROM " + quotedTableName + " WHERE " + quotedFilterColumn + " = ? LIMIT 1;";

        List<String> results = new ArrayList<>();

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, filterValue);

            try (ResultSet resultSet = pst.executeQuery()) {
                if (resultSet.next()) { // Ensure there's a result before accessing data
                    for (int i = 0; i < columns.length; i++) {
                        results.add(resultSet.getString(i + 1)); // Get values by index
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving filtered values: " + e.getMessage());
        }

        return results;
    }


    public boolean deleteRowFromTable(String tableName, String filterColumn, String filterValue) throws SQLException {
        // Quote table and column names if they contain spaces
        String quotedTableName = tableName.contains(" ") ? "\"" + tableName + "\"" : tableName;
        String quotedFilterColumn = filterColumn.contains(" ") ? "\"" + filterColumn + "\"" : filterColumn;

        // Build the SQL query
        String sql = "DELETE FROM " + quotedTableName + " WHERE " + quotedFilterColumn + " = ?;";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            // Set the filter value in the query
            pst.setString(1, filterValue);

            // Execute the query and return if any row was affected
            int rowsAffected = pst.executeUpdate();
            return rowsAffected > 0; // Return true if a row was deleted
        } catch (SQLException e) {
            System.out.println("Error deleting row: " + e.getMessage());
            return false; // Return false if there was an error
        }
    }

    //working
    public boolean belongsToTable(String tableName, String field) throws SQLException {
        // Get the list of columns dynamically
        List<String> columns = this.getTableHeaders(tableName);

        // Quote table name to handle spaces
        String quotedTableName = tableName.contains(" ") ? "\"" + tableName + "\"" : tableName;

        // Iterate through each column to check if the field exists in any of them
        for (String column : columns) {
            String quotedColumn = column.contains(" ") ? "\"" + column + "\"" : column;
            String sql = "SELECT 1 FROM " + quotedTableName + " WHERE " + quotedColumn + " = ? LIMIT 1";

            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, field);

                try (ResultSet result = pst.executeQuery()) {
                    if (result.next()) {
                        return true; // Field exists in at least one column
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error checking table values: " + e.getMessage());
            }
        }

        return false; // No matching value found in any column of the table
    }



    //working
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

    //working
    public List<String> getColumnValues(String tableName, String columnName) throws SQLException {
        List<String> values = new ArrayList<>();

        // Quote column and table names if they contain spaces
        String quotedTableName = tableName.contains(" ") ? "\"" + tableName + "\"" : tableName;
        String quotedColumnName = columnName.contains(" ") ? "\"" + columnName + "\"" : columnName;

        // Prepare the SQL query with quoted column and table names
        String sql = "SELECT " + quotedColumnName + " FROM " + quotedTableName;

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet result = pst.executeQuery()) {

            while (result.next()) {
                String value = result.getString(1); // Get the first (and only) column value

                // Add the value to the list only if it is not null or empty
                if (value != null && !value.trim().isEmpty()) {
                    values.add(value);
                }
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

    public List<String> getColumnValuesByFilter(String tableName, String columnToSelect, String filterColumn, String filterValue) throws SQLException {
        // Quote table and column names if they contain spaces
        String quotedTableName = tableName.contains(" ") ? "\"" + tableName + "\"" : tableName;
        String quotedColumnToSelect = columnToSelect.contains(" ") ? "\"" + columnToSelect + "\"" : columnToSelect;
        String quotedFilterColumn = filterColumn.contains(" ") ? "\"" + filterColumn + "\"" : filterColumn;

        // SQL query to select values from columnToSelect where filterColumn contains filterValue
        String sql = "SELECT " + quotedColumnToSelect + " FROM " + quotedTableName + " WHERE " + quotedFilterColumn + " LIKE ?;";

        List<String> resultList = new ArrayList<>();

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            // Set the filter value to search for substrings
            pst.setString(1, "%" + filterValue + "%");

            // Execute the query and process the result set
            try (ResultSet resultSet = pst.executeQuery()) {
                while (resultSet.next()) {
                    resultList.add(resultSet.getString(1)); // Get the value from the specified column
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving column values: " + e.getMessage());
        }

        return resultList;
    }

    public void createTable(String tableName, LinkedHashMap<String, String> columns) throws SQLException {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Column list cannot be empty.");
        }

        // Quote the table name if it contains spaces
        String quotedTableName = tableName.contains(" ") ? "\"" + tableName + "\"" : tableName;

        // Build the column definitions string in the order of insertion
        StringBuilder columnDefinitions = new StringBuilder();
        for (Map.Entry<String, String> entry : columns.entrySet()) {
            String column = entry.getKey().replaceAll("\\s", ""); // Remove spaces from column names
            String type = entry.getValue().toUpperCase(); // Ensure proper casing for SQL types
            columnDefinitions.append("\"").append(column).append("\" ").append(type);
            columnDefinitions.append(", ");
        }

        // Remove trailing comma and space
        if (columnDefinitions.length() > 2) {
            columnDefinitions.setLength(columnDefinitions.length() - 2);
        }

        // Construct the CREATE TABLE SQL query
        String sql = "CREATE TABLE IF NOT EXISTS " + quotedTableName + " (" + columnDefinitions + ");";

        // Execute the query
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table '" + tableName + "' created successfully.");
        } catch (SQLException e) {
            System.out.println("Error creating table: " + e.getMessage());
        }
    }

    public void addColumnsToTable(String tableName, LinkedHashMap<String, String> columns) throws SQLException {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Column list cannot be empty.");
        }

        // Quote the table name if it contains spaces
        String quotedTableName = tableName.contains(" ") ? "\"" + tableName + "\"" : tableName;

        // Iterate through the LinkedHashMap to maintain column order
        for (Map.Entry<String, String> entry : columns.entrySet()) {
            String columnName = entry.getKey().replaceAll("\\s", ""); // Remove spaces from column names
            String columnType = entry.getValue().toUpperCase(); // Ensure proper casing for SQL types

            // SQL statement to add a single column
            String sql = "ALTER TABLE " + quotedTableName + " ADD COLUMN \"" + columnName + "\" " + columnType + ";";

            // Execute the query
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println("Added column '" + columnName + "' to table '" + tableName + "'.");
            } catch (SQLException e) {
                System.out.println("Error adding column '" + columnName + "' to table '" + tableName + "': " + e.getMessage());
            }
        }
    }

    //public static void main(String[] args) throws SQLException {
      //  DatabaseManager db = new DatabaseManager("/home/user/test.db");
        /*LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "INTEGER PRIMARY KEY");
        columns.put("name", "TEXT");
        columns.put("age", "TEXT");
        db.createTable("testTable2", columns);
        System.out.println("=====================================================");
        LinkedHashMap<String, String> columns1 = new LinkedHashMap<>();
        columns1.put("ID", "INTEGER PRIMARY KEY");
        columns1.put("First Name", "TEXT");
        columns1.put("Last Name", "TEXT");
        columns1.put("Email", "TEXT UNIQUE");
        columns1.put("Age", "INTEGER");
        columns1.put("Salary", "REAL");

        db.createTable("Employees", columns1);*/

        /*LinkedHashMap<String, String> newColumns = new LinkedHashMap<>();
        newColumns.put("Phone Number", "TEXT");
        newColumns.put("Hire Date", "DATE");
        newColumns.put("Salary", "REAL");

        db.addColumnsToTable("Employees", newColumns);*/

    }

    /*public static void main(String args[]) throws SQLException {
        DatabaseManager db = new DatabaseManager("/home/user/test.db");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Do you want to import the Excel file? (yes/no): ");
        String response = scanner.nextLine().trim().toLowerCase();

        if (response.equals("yes") || response.equals("y")) {
            db.importXlsxToDatabase("/home/user/QubesIncoming/school/UMS_Data.xlsx");
            System.out.println("Import successful.");
        } else {
            System.out.println("Import canceled.");
        }

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

        System.out.println("\nColumn Values");
        List<String> values = db.getColumnValues(tables.get(0), headers.get(0));
        for (String value : values) {
            System.out.println(value);
        }

        if(db.belongsToTable("Subjects", "MATH001")) {
            System.out.println("Belongs to Table");
        }else{
            System.out.println("Not Belongs to Table");
        }

        System.out.println("\nrow");
        List<String> row = db.getRow("Subjects", "Subject Code", "MATH001");
        for (String value : row) {
            System.out.println(value);
        }

        System.out.println("\nNames: ");
        String[] columns = {"Name", "Degree"};
        List<String> data = db.getFilteredValues("Faculties", columns, "Faculty ID", "F0001");

       for (String value : data) {
           System.out.println(value);
       }



        String[] newVals = {"test4", "test2"};
        db.addRowToTable("Subjects", newVals);

        System.out.println("\nRow added");
        values = db.getColumnValues("Subjects", "Subject Code");
        for (String value : values) {
            System.out.println(value);
        }

        System.out.println("Updating row");

        List<String> vals = new ArrayList<>();
        vals.add("Testing");
        vals.add("Tests");

        db.updateRowInTable("Subjects", "Subject Code", "test4", vals);
        values = db.getColumnValues("Subjects", "Subject Code");
        for (String value : values) {
            System.out.println(value);
        }


        System.out.println("\nDeleting row");
        db.deleteRowFromTable("Faculties", "Faculty ID", "F0001");
        values = db.getColumnValues("Faculties", "Faculty ID");
        for (String value : values) {
            System.out.println(value);
        }

    }*/
//}
