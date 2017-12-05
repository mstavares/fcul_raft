package server;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import common.OperationType;
import server.models.LogEntry;
import server.models.RaftStatus;
import utilities.Debugger;

import static utilities.Utilities.getFileSeparator;

class FileManager {
	
	/** Folder name */
	private static final String DATABASE_FOLDER = "database";
	
    /** Log file name */
    private static final String LOG_FILE_NAME = "database.log";

    /** Log file path */
    private static final String LOG_PATH = DATABASE_FOLDER + getFileSeparator() + LOG_FILE_NAME;

    /** String used to separate data on logs */
    private static final String SEP_STR = " ";
    
    
	/** Data file name */
	private static final String DATABASE_FILE_NAME = "database.dat";

	/** Data file path */
	private static final String DATABASE_PATH = DATABASE_FOLDER + getFileSeparator() + DATABASE_FILE_NAME;

	/** Backup file name */
	private static final String DATABASE_BAK_FILE_NAME = "database.bak";
	
	/**Backup file path */
	private static final String DATABASE_BAK_PATH = DATABASE_FOLDER + getFileSeparator() + DATABASE_BAK_FILE_NAME;
    
	
    /**
     * Writes the database structure to disk in a safe way.
     * WARNING: This deletes the log file without applying it on the structure.
     */
	public void writeDatabaseToFile(RaftStatus database) throws IOException {
		Debugger.log("Started writing map to file");
		File datFile = new File(DATABASE_PATH);
		File bakFile = new File(DATABASE_BAK_PATH);
		File logFile = new File(LOG_PATH);
		
		if (bakFile.exists()){
			bakFile.delete();
			Debugger.log("Deleted database.bak file.");
		}
		
		Debugger.log("Started writing database.bak file.");
		FileOutputStream fos = new FileOutputStream(bakFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(database);
		oos.flush();
		oos.close();
		fos.close();
		Debugger.log("Finished writing database.bak file.");
		
		if (logFile.exists()){
			logFile.delete();
			Debugger.log("Deleted database.log file.");
		}
		
		if (datFile.exists()){
			datFile.delete();
			Debugger.log("Deleted database.dat file.");
		}
		
		if (bakFile.renameTo(datFile) ){
			Debugger.log("Renamed database.bak to database.dat");
		}else{
			Debugger.log("Error renaming database.bak to database.dat");
		}
		
		Debugger.log("Finished writing map to file");
	}
	
	
    private void writeToLog(String log) throws IOException {
    	FileWriter writer = new FileWriter(LOG_PATH, true);
        writer.write(log + "\n");
        writer.flush();
        writer.close();
    }
    
    
    private void processLine(RaftStatus database, String[] parsedData) {
        OperationType op = OperationType.valueOf(parsedData[0]);
        int term = Integer.parseInt(parsedData[1]);
        String key = parsedData[2];
        String oldValue = parsedData[3];
        String newValue = parsedData[4];
        
        if(key.equals("null"))
        	key = null;
        
        if(oldValue.equals("null"))
        	oldValue = null;
        
        if(newValue.equals("null"))
        	newValue = null;
        
        LogEntry logEntry = new LogEntry(op, term, key, oldValue, newValue);
        database.getLogs().add(logEntry);
    }
    
    private void processLine(Log logs, String[] parsedData) {
    	OperationType op = OperationType.valueOf(parsedData[0]);
        int term = Integer.parseInt(parsedData[1]);
        String key = parsedData[2];
        String oldValue = parsedData[3];
        String newValue = parsedData[4];
        
        if(key.equals("null"))
        	key = null;
        
        if(oldValue.equals("null"))
        	oldValue = null;
        
        if(newValue.equals("null"))
        	newValue = null;
        
        logs.add(new LogEntry(op, term, key, oldValue, newValue));
    }
    
    
    public void appendOperationToLog(OperationType op, int term, String key, String oldValue, String newValue) throws IOException {
    	writeToLog(op + SEP_STR + term + SEP_STR + key + SEP_STR + oldValue + SEP_STR + newValue);
    }
    
    public void recoverStatusFromLog(Log logs) throws IOException {
    	FileReader fileReader = new FileReader(LOG_PATH);
        BufferedReader bufRead = new BufferedReader(fileReader);
        String line = null;
        while ((line = bufRead.readLine()) != null) {
            String[] parsedData = line.split(SEP_STR);
            processLine(logs, parsedData);
        }
        bufRead.close();
        
        Debugger.log("Finished updating Log with logfile");
    }
    
    /**
     * Updates the database structure given with the log entries.
     * The update structure is automatically saved to disk in a safe way.
     */
    private void updateDatabaseFromLog(RaftStatus database) throws IOException {
    	Debugger.log("Started updating datase with log");
    	
    	FileReader fileReader = new FileReader(LOG_PATH);
        BufferedReader bufRead = new BufferedReader(fileReader);
        String line = null;
        while ((line = bufRead.readLine()) != null) {
            String[] parsedData = line.split(SEP_STR);
            processLine(database, parsedData);
        }
        bufRead.close();
        
        Debugger.log("Finished updating database with log");
        writeDatabaseToFile(database);
    }
    

    
	private void existsDatabaseFolder() {
		File folder = new File(DATABASE_FOLDER);
		if (!folder.exists()){
			Debugger.log("Creating the database folder");
			new File(DATABASE_FOLDER).mkdir();
		}
	}

	private RaftStatus createEmptyDatabase() {
		Debugger.log("Started creating empty database.");
		RaftStatus database = new RaftStatus();
		Debugger.log("Created database sucessfully.");
		return database;
	}
	
	
	public RaftStatus restoreDatabase() throws IOException {
		Debugger.log("Restoring database from files");
		
		existsDatabaseFolder();
		
		RaftStatus database;
		File datFile = new File(DATABASE_PATH);
		File bakFile = new File(DATABASE_BAK_PATH);
		File logFile = new File(LOG_PATH);
		
		if ( datFile.exists() ){
			if ( logFile.exists() ){
				if ( bakFile.exists() ){
					//dat + log + bak
					database = restoreDat();
					updateDatabaseFromLog(database);
				}else{
					//dat + log
					database = restoreDat();
					updateDatabaseFromLog(database);
				}
			}else{
				if ( bakFile.exists() ){
					//dat + bak
					datFile.delete();
					bakFile.renameTo(datFile);
					database = restoreDat();
				}else{
					//dat
					database = restoreDat();
				}
			}
		}else{
			if ( logFile.exists() ){
				if ( bakFile.exists() ){
					//log + bak
					database = createEmptyDatabase();
					updateDatabaseFromLog(database);
				}else{
					//log
					database = createEmptyDatabase();
					updateDatabaseFromLog(database);
				}
			}else{
				if ( bakFile.exists() ){
					//bak
					bakFile.renameTo(datFile);
					database = restoreDat();
				}else{
					//nothing
					database = createEmptyDatabase();
					writeDatabaseToFile(database);
					
				}
			}
		}
		
		Debugger.log("Restored Database from files sucessfully.");
		return database;
	}
	
	
	private RaftStatus restoreDat() throws IOException {
		Debugger.log("Restoring .dat file");
		RaftStatus database;
		File databaseFile = new File(DATABASE_PATH);
		FileInputStream fis = new FileInputStream(databaseFile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		try {
			database = (RaftStatus) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new IOException("Error reading database file.");
		}finally{
			ois.close();
			fis.close();
		}
		Debugger.log("Finished restoring .dat file");
		return database;
	}
}
