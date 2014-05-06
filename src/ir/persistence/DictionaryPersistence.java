package ir.persistence;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Hashtable;

import edu.smu.tspell.wordnet.SynsetType;

public class DictionaryPersistence {
	DecimalFormat formatter = new DecimalFormat("#0.00000");
	public static Hashtable<String, Double> hashDictionary = new Hashtable<String, Double>();
	public StringBuilder dataDictionary = new StringBuilder("");
	public int delimiterCount = 0;
	public static Object nextRecordLock = new Object();
	public static Object hashDictionaryLock = new Object();
	static String database = "jdbc:mysql://localhost:3306/Dictionary";
	static String user = "root";
	static String pwd = "mypassword";
	Connection conn = null;
	public String baseSeed = null;
	public boolean isDispute = false;
	public String workingSeed=null;

	public DictionaryPersistence(String baseSeed,String wordkingSeed, boolean isDispute) {
		this.baseSeed = baseSeed;
		this.isDispute = isDispute;
		this.workingSeed=wordkingSeed;
		
	}

	public boolean saveToHash(String word, double similarity) {
		synchronized (hashDictionaryLock) {
			if (!hashDictionary.containsKey(word)) {
				hashDictionary.put(word, similarity);
				return true;
			}
			return false;
		}

	}

	public void save(String word, SynsetType type, double similarity) {
		if (saveToHash(word, similarity)) {
			dataDictionary.append(word + "," + formatter.format(similarity));
			delimiterCount++;
			if (delimiterCount > 100) {
				System.out.println("greater than :"
						+ Thread.currentThread().getName());
				saveWordstoDB(type);
			} else {
				dataDictionary.append(";");
			}
		}
	}

	public boolean isWordAlreadyExists(String seed, String word) {
		synchronized (hashDictionaryLock) {
			return hashDictionary.containsKey(seed + word);
		}
	}

	public boolean saveWordstoDB(SynsetType type) {
		String rootWord="";
		if(isDispute)
			rootWord=workingSeed;		
			
		if (dataDictionary.length() > 0) {
			String s = "{call insertDictionary('" + baseSeed + "','"
					+ getSynsetTypeString(type) + "','"
					+ dataDictionary.toString().replace("'", "") + "',"
					+ delimiterCount + "," + (isDispute ? 1 : 0) + ",'"+rootWord+"')}";
			try {
				// System.out.println(dataDictionary.toString());
				System.out.println("Delimiter :" + delimiterCount);				
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				conn = DriverManager.getConnection(database, user, pwd);
				CallableStatement cs = conn.prepareCall(s);
				cs.execute();
				dataDictionary = new StringBuilder("");
				delimiterCount = 0;
				return true;
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
				// ex.printStackTrace();
				System.out.println("s=:" + s);
				System.out.println("exception than :"
						+ Thread.currentThread().getName());
				System.exit(0);
			} finally {
				try {
					conn.close();
				} catch (Exception e) {
				}
			}
			return false;
		}
		return true;
	}

	public static String getSynsetTypeString(SynsetType type) {
		if (type == SynsetType.VERB)
			return "VERB";
		else if (type == SynsetType.ADJECTIVE)
			return "ADJECTIVE";
		else if (type == SynsetType.NOUN)
			return "NOUN";
		else if (type == SynsetType.ADVERB)
			return "ADVERB";
		return "";
	}

	public String getNextWord(SynsetType type, int changeType) {
		int id = 0;
		String nextWord = null;
		String stype = null;
		String nextSeed = null;
		String s = null;
		String rootWord=null;
		try {
			synchronized (nextRecordLock) {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				conn = DriverManager.getConnection(database, user, pwd);
				s = "{call getNextRecord('" + getSynsetTypeString(type) + "',"
						+ changeType + "," + (isDispute ? 1 : 0) + ")}";
				CallableStatement cs = conn.prepareCall(s);
				ResultSet rs = cs.executeQuery();
				while (rs.next()) {
					id = rs.getInt("nextid");
					nextWord = rs.getString("nextword");
					stype = rs.getString("stype");
					nextSeed = rs.getString("nextseed");
					if(isDispute)
						rootWord=rs.getString("nextroot");
				}
				return id + "-" + nextWord + "-" + stype + "-" + nextSeed+"-"+rootWord;
			}
		} catch (Exception ex) {
			System.out.println("sss==" + s);
			ex.printStackTrace();
			System.out.println(ex.getMessage());
			System.exit(0);
		} finally {
			try {
				conn.close();
			} catch (Exception e) {

			}
		}
		return null;
	}

	public void updateProcessedRecords(int id) {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(database, user, pwd);
			Statement stmt = conn.createStatement();
			String table = "supportDictionary";
			if (isDispute)
				table = "disputeDictionary";
			stmt.executeUpdate("update " + table + " set processed=2 where id="
					+ id);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		} finally {
			try {
				conn.close();
			} catch (Exception e) {

			}
		}
	}

}
