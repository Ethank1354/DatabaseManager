class to make accessing and reading from a database easier in other programs.

To use:
in intellij:
press ctrl + alt + shift + s
navigate to modules > dependencies
click the +
add from jar or directory
select the jar file from releases

Usage:

import com.ethankiers.databasemanagement.databasemanager.DatabaseManager;

the constructor is looking for the path to the sqlite db file as a string. Absolute or relative should work.

Methods:
boolean importXLSXToDatabase
  takes the path of the excel file as a string, returns true if it worked and false if it didnt, it will also print the sql error if one occurs

boolean addRowToTable
  takes the table name as a String, and a string array of the values to be added. make sure there is a value for every column in the table

boolean updateRowInTable
  takes in the table name, filter column name and filter value as Strings and a List<String> of the new values, same warning as above method


List<String> getRow 
  takes table name, filter column and value as Strings
  returns a List of the records associated with that value

List<String> getFilteredValues
  takes the table name, filter column and value as Strings and a String array of the columns to get data from
  returns a list containing data from the specified columns. like the method above, but instead of returning the whole row, it returns only the data in the row from the specified columns

boolean deleteRowFromTable
  takes table name, filter column and value as String and deletes the corresponding row

boolean belongsToTable
  checks if a specified value belongs to a specified table,, returns true if so and false if not.

List<String> getTableHeaders
  takes the table name as a string and returns a list of the column names for that table

List<String> getColumnValues
  takes the table name and column as strings
  returns all of the values in that column

List<String> getTables
  returns a list of the tables in the database

depends on jdbc sqlite driver, apache poi aand apache poi ooxml, they are packaged in the release jar, so they don't need to be added separately if using that.
