package ir.support;



import ir.persistence.DictionaryPersistence;

import java.util.ArrayList;
import java.util.Random;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class SupportFinderThread implements Runnable {

	int id;
	String word;
	String seed = null;
	SynsetType currentWordType;
	SynsetType seedType;
	DictionaryPersistence dp = null;
	int proceesWordCnt = 0;
	ArrayList<String> wordsList = null;
	Random r = new Random();
	boolean diffType=false;
	SimilarityCalculator simCalc=null;

	public SupportFinderThread(String seed,SynsetType seedType, boolean diffType,boolean isDispute) {
		this.seed = seed;
		dp = new DictionaryPersistence(seed,"", isDispute);
		wordsList = new ArrayList<String>();
		this.diffType=diffType;
		this.seedType=seedType;
		this.simCalc=new SimilarityCalculator(dp);
	}

	public void run() {		
		System.out.println(Thread.currentThread().getName());
		while (true) {
			if (getNextRecord()) {
				startProcess();
				proceesWordCnt++;
				System.out.println("Get Next Record:"+Thread.currentThread().getName());
				dp.saveWordstoDB( currentWordType); 
			}
			if (proceesWordCnt > 20) {
				break;
			}
		}
	}

	public boolean getNextRecord() {
		String nextWord = dp.getNextWord(seedType,(diffType?1:0));
		if (nextWord != null) {
			String[] splitArr = nextWord.split("-");
			id = Integer.parseInt(splitArr[0]);
			word = splitArr[1];
			currentWordType=getSynsetType(splitArr[2]);
			this.seed=splitArr[3];			
			this.dp.baseSeed=this.seed;
			if(this.dp.isDispute && splitArr.length>4){
				this.dp.workingSeed=splitArr[4];
				//Root Word is working seed
				this.seed=splitArr[4];
			}
			return true;
		}
		return false;
	}
	
	public static SynsetType getSynsetType(String type){
		if(type.equals("VERB"))
			return SynsetType.VERB;
		else if(type.equals("NOUN"))
			return SynsetType.NOUN;
		else if(type.equals("ADVERB"))
			return SynsetType.ADVERB;
		else
			return SynsetType.ADJECTIVE;
		
	}

	public void startProcess() {
		// Gather ALl the words. It includes Sense words,hypernyms,troponyms.
		gatherWords();
		for (String word : wordsList) {	
			if(seed.equals("VERB")){
				System.out.println();
			}
			if(!word.equals("believe"))			
			//computeSimilarityWOCommonSense(word);
			simCalc.distSenseSimilarity(seed, word, currentWordType);		
		}
		dp.updateProcessedRecords(id);
	}

	private void gatherWords() {
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		Synset[] synsets = database.getSynsets(word, currentWordType);
		// Sense words
		for (Synset synset : synsets) {
			gatherSenseWords(synset);
			gatherHypernyms(synset);
			gatherTroponyms(synset);
		}

	}

	private void gatherSenseWords(Synset synset) {
		String[] wordsArr = synset.getWordForms();
		for (String word : wordsArr) {
			wordsList.add(word);
		}
	}

	private void gatherHypernyms(Synset synset) {
		if (currentWordType == SynsetType.VERB) {
			VerbSynset vSynset = (VerbSynset) synset;
			VerbSynset[] hypernymsArr = vSynset.getHypernyms();
			for (VerbSynset hypernymSet : hypernymsArr) {
				String[] wordsArr = hypernymSet.getWordForms();
				for (String word : wordsArr) {
					wordsList.add(word);
				}
				gatherHypernyms(hypernymSet);
			}
		} else if (currentWordType == SynsetType.NOUN) {
			NounSynset nSynset = (NounSynset) synset;
			NounSynset[] hypernymsArr = nSynset.getHypernyms();
			for (NounSynset hypernymSet : hypernymsArr) {
				String[] wordsArr = hypernymSet.getWordForms();
				for (String word : wordsArr) {
					wordsList.add(word);
				}
				gatherHypernyms(hypernymSet);
			}
		}
	}

	private void gatherTroponyms(Synset synset) {
		if (currentWordType == SynsetType.VERB) {
			VerbSynset vSynset = (VerbSynset) synset;
			VerbSynset[] hypernymsArr = vSynset.getTroponyms();
			for (VerbSynset hypernymSet : hypernymsArr) {
				String[] wordsArr = hypernymSet.getWordForms();
				for (String word : wordsArr) {
					wordsList.add(word);
				}
				gatherTroponyms(hypernymSet);
			}
		}
	}

//	private void computeSimilarityWOCommonSense(String word) {
//	
//		//	System.out.println(Thread.currentThread().getName()+ ": Case 3 Similarity using WORD: :"+this.word+";;; seed: " + seed + ", " + word
//		//			+ ", " + type.toString());
//			dp.save( word,currentWordType, r.nextDouble());
//	}
}
