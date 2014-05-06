
import ir.persistence.DictionaryPersistence;
import ir.support.SupportDictForm;
import ir.support.SupportFinderThread;

import java.io.FileOutputStream;
import java.io.PrintStream;

import edu.smu.tspell.wordnet.SynsetType;

public class DictionaryCreator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PrintStream out = null;
		try{
		out = new PrintStream(new FileOutputStream("c:\\Guru\\output.txt"));
		System.setOut(out);
		String seed="believe";
		SynsetType type=SynsetType.VERB;
		System.setProperty("wordnet.database.dir",
				"C:\\Program Files\\WordNet\\2.1\\dict\\");
		SupportDictForm s = new SupportDictForm(type);
		s.startProcess(seed, SynsetType.VERB);

		Thread t1=new Thread(new SupportFinderThread(seed,type,false),"Thread11111111");
		Thread t2=new Thread(new SupportFinderThread(seed,type,false),"Thread22222222");
		Thread t3=new Thread(new SupportFinderThread(seed,type,true),"Thread33333333");
		t1.start();
		
		//t1.join();
		
		t2.start();
		t3.start();
		
		//System.out.println(DictionaryPersistence.hashDictionary.toString());
		System.out.println(DictionaryPersistence.hashDictionary.size());
		} catch(Exception ex){
			System.out.println(ex.getMessage());
		}finally{
			out.close();
		}
	}

}
