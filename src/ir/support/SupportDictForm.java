package ir.support;

import ir.persistence.DictionaryPersistence;
import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.WordSense;

public class SupportDictForm {
	DictionaryPersistence dp = null;
	SimilarityCalculator simCalc = null;
	boolean isDispute = false;
	SynsetType type;

	public SupportDictForm() {

	}

	public SupportDictForm(SynsetType type, String baseSeed, boolean isDispute) {
		this.type = type;
		this.dp = new DictionaryPersistence(baseSeed,
				isDispute ? baseSeed : "", isDispute);
		simCalc = new SimilarityCalculator(dp);
		this.isDispute = isDispute;
	}

	public void startProcess(String seed, SynsetType type) {
		// Get all the Synset Words
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		this.dp.workingSeed = seed;
		Synset[] synsets = database.getSynsets(seed, type);
		// Iterate thru all the senses now..
		// These are the words which are in same sense.
		for (Synset synset : synsets) {
			String[] wordsArr = synset.getWordForms();
			for (String word : wordsArr) {
				// Compute Similarity. CASE 1.
				simCalc.sameSenseSimilarity(seed, word, synset, type);
			}
		}

		// Iterate thru all the senses now..
		// Now you are iterating thru all the hypernyms.
		for (Synset synset : synsets) {
			handleHypernyms(seed, synset, type);
		}

		// Iterate thru all the senses now..
		// Now you are iterating thru all the troponyms.
		for (Synset synset : synsets) {
			handleTroponyms(seed, synset, type);
		}

		// Iterate thru all the senses now..
		// Now you are iterating thru all the Hyponyms.
		for (Synset synset : synsets) {
			handleHyponyms(seed, synset, type);
		}

		dp.saveWordstoDB(type);

		// Before handling the Derivationally Realted words push all the
		// calculated words to DB.
		// Clear Data Dictionary.
		if (!isDispute) {
			handleDeriRWords(seed, type);
			dp.saveWordstoDB(type);
		}
	}

	// private void computeSimilarityWithSameSense(String seed, String word,
	// Synset synset, SynsetType type) {
	//
	// System.out.println("Same Sense Similarity between:" + seed + ", "
	// + word);
	// dp.save(seed, word,type, r.nextDouble());
	// }

	private void handleHypernyms(String seed, Synset synset, SynsetType type) {
		if (type == SynsetType.VERB) {
			VerbSynset vSynset = (VerbSynset) synset;
			VerbSynset[] hypernymsArr = vSynset.getHypernyms();
			for (VerbSynset hypernymSet : hypernymsArr) {
				String[] wordsArr = hypernymSet.getWordForms();
				for (String word : wordsArr) {
					// Calculate similarity where the both words has hypernym
					// relationship
					simCalc.hypernymSimilarity(seed, word, type);
				}
				handleHypernyms(seed, hypernymSet, type);
			}
		} else if (type == SynsetType.NOUN) {
			NounSynset nSynset = (NounSynset) synset;
			NounSynset[] hypernymsArr = nSynset.getHypernyms();
			for (NounSynset hypernymSet : hypernymsArr) {
				String[] wordsArr = hypernymSet.getWordForms();
				for (String word : wordsArr) {
					// Calculate similarity where the both words has hypernym
					// relationship
					simCalc.hypernymSimilarity(seed, word, type);
				}
				handleHypernyms(seed, hypernymSet, type);
			}
		}
	}

	private void handleTroponyms(String seed, Synset synset, SynsetType type) {
		if (type == SynsetType.VERB) {
			VerbSynset vSynset = (VerbSynset) synset;
			VerbSynset[] hypernymsArr = vSynset.getTroponyms();
			for (VerbSynset hypernymSet : hypernymsArr) {
				String[] wordsArr = hypernymSet.getWordForms();
				for (String word : wordsArr) {
					// Calculate similarity where the both words has hypernym
					// relationship
					simCalc.troponymSimilarity(seed, word, type);
				}
				handleTroponyms(seed, hypernymSet, type);
			}
		}
	}

	private void handleHyponyms(String seed, Synset synset, SynsetType type) {
		if (type == SynsetType.NOUN) {
			NounSynset hSynset = (NounSynset) synset;
			NounSynset[] hyponymsArr = hSynset.getHyponyms();
			for (NounSynset hyponymSet : hyponymsArr) {
				String[] wordsArr = hyponymSet.getWordForms();
				for (String word : wordsArr) {
					// Calculate similarity where the both words has hyponyms
					// relationship
					simCalc.hyponymSimilarity(seed, word, type);
				}
				handleHyponyms(seed, hyponymSet, type);
			}
		}
	}

	// private void computeSimilarityWithTroponyms(String seed, String word,
	// SynsetType type) {
	//
	// System.out.println("Troponym Similarity :" + seed + ", " + word + ", "
	// + type.toString());
	// dp.save(seed, word,type,r.nextDouble());
	//
	// }

	private void handleDeriRWords(String seed, SynsetType type) {
		// BAse condition
		if (this.type != type)
			return;
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		Synset[] synsetArr = database.getSynsets(seed);
		for (Synset synset : synsetArr) {
			WordSense[] wordSenseArr = synset
					.getDerivationallyRelatedForms(seed);
			for (WordSense wordSense : wordSenseArr) {
				this.dp.baseSeed = wordSense.getWordForm();
				startProcess(wordSense.getWordForm(), wordSense.getSynset()
						.getType());
			}
		}
	}
}
