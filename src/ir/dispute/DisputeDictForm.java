package ir.dispute;


import ir.support.SupportDictForm;
import ir.support.SupportFinderThread;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.WordSense;

public class DisputeDictForm {

	SynsetType type;
	SupportDictForm supportDictForm = null;
	String seed=null;

	public DisputeDictForm(SynsetType type, String baseSeed) {
		this.type = type;
		this.supportDictForm = new SupportDictForm(this.type, baseSeed, true);
		this.seed=baseSeed;
	}

	public void statDisputeProcess() {
		// Get all the Synset Words
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		Synset[] synsets = database.getSynsets(seed, type);
		for (Synset synset : synsets) {
			WordSense[] wordArr = synset.getAntonyms(seed);
			for (WordSense wordSense : wordArr) {
				for (String word : wordSense.getSynset().getWordForms()) {
					supportDictForm.startProcess(word, wordSense.getSynset()
							.getType());
				}
			}
		}
		Thread disputeThread=new Thread(new SupportFinderThread(null, this.type, false, true),"Thread Dispute");
		disputeThread.start();
	}
}
