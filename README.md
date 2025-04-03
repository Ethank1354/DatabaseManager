# DatabaseManager

DatabaseManager is a Java class designed to simplify accessing and interacting with SQLite databases in other programs. It provides convenient methods for importing data, querying, updating, and managing database records.

## Installation

### IntelliJ Setup
### From Jar
1. Press `Ctrl + Alt + Shift + S` to open **Project Structure**.
2. Navigate to **Modules > Dependencies**.
3. Click the `+` button and select **Add from JAR or directory**.
4. Choose the JAR file from the **Releases** section.
5. Import the Library

```java
import com.ethankiers.databasemanagement.databasemanager.DatabaseManager;
```

### In An Intellij JavaFX Project
1. Download the .java file from releases
2. Download the latest jdbc driver .jar from here: https://github.com/xerial/sqlite-jdbc/releases
3. Move it to the lib folder in your project (Projectname/lib)
4. Press `Ctrl + Alt + Shift + S` to open **Project Structure**.
5. Navigate to **Librairies**
6. Click the `+` button and select **JAVA**.
7. Select the jdbc .jar file
8. Follow the prompts and then click the `+` again
9. Select **From Maven**
10. Paste org.apache.poi:poi:5.4.0 in the box
11. Select the `download to` checkbox and click ok
12. Follow the prompts and then click the `+` again
13. Select **From Maven**
14. Paste org.apache.poi:poi-ooxml:5.4.0 in the box
15. Select the `download to` checkbox and click ok
16. Copy the .java file that you downloaded to the main package of your project.
17. Fix the package name and follow Intellij's prompts to resolve the errors

## Usage

### Constructor
The `DatabaseManager` constructor requires the path to the SQLite database file as a `String`. You can use either an absolute or relative path.

```java
DatabaseManager dbManager = new DatabaseManager("path/to/database.db");
```

## Available Methods

### Data Import
```java
boolean importXLSXToDatabase(String excelFilePath);
```
- Imports data from an Excel file into the database.
- Returns `true` if successful, otherwise `false`.
- Prints SQL errors if any occur.

### Data Manipulation
```java
boolean addRowToTable(String tableName, String[] values);
```
- Adds a new row to the specified table.
- Ensure that the number of values matches the number of columns.

```java
boolean updateRowInTable(String tableName, String filterColumn, String filterValue, List<String> newValues);
```
- Updates an existing row in the specified table.
- `filterColumn` and `filterValue` determine which row to update.
- `newValues` should contain the updated values for all columns.

```java
boolean deleteRowFromTable(String tableName, String filterColumn, String filterValue);
```
- Deletes a row from the specified table based on the filter condition.

```java
void createTable(String tableName, LinkedHashMap columns);
```
- Creates a new table with the specified name and columns
- Takes a linked hashmap in the form of <String, String> where the first element is the column name and the second is the data type
  - TEXT
  - REAL
  - INTEGER
  - BLOB

 ```java
void addColumnToTable(String Table, LinkedHashMap columns)
```
- Adds the specified columns to the specified table
- Takes a linked hashmap in the form of <String, String> where the first element is the column name and the second is the data type
  - TEXT
  - REAL
  - INTEGER
  - BLOB
 
### Data Retrieval
```java
List<String> getRow(String tableName, String filterColumn, String filterValue);
```
- Retrieves a full row matching the filter condition.

```java
List<String> getFilteredValues(String tableName, String[] columns, String filterColumn, String filterValue);
```
- Retrieves specific column values from rows matching the filter condition.

```java
List<String> getTableHeaders(String tableName);
```
- Returns a list of column names for the specified table.

```java
List<String> getColumnValues(String tableName, String columnName);
```
- Returns all values from a specific column.

```java
List<String> getTables();
```
- Returns a list of all tables in the database.

```java
List<String> getColumnValuesByFilter(String tableName, String columnToSelect, String filterColumn, String filterValue);
```
- Returns a list of the values in a column given the name and filter of another column

### Validation
```java
boolean belongsToTable(String tableName, String value);
```
- Checks if a specified value exists in the given table.
- Returns `true` if found, otherwise `false`.

## Dependencies
DatabaseManager relies on the following libraries:
- **JDBC SQLite Driver**
- **Apache POI** (for Excel file handling)
- **Apache POI OOXML**

These dependencies are bundled in the release JAR, so no additional setup is needed if using the pre-built release.


