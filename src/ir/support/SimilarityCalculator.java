package ir.support;

import java.util.HashMap;
import java.util.Map;

import ir.persistence.DictionaryPersistence;
import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class SimilarityCalculator {
	WordNetDatabase database = WordNetDatabase.getFileInstance();
	Synset[] seedArr = null;
	Synset[] wordArr = null;
	DictionaryPersistence dp = null;

	public SimilarityCalculator(DictionaryPersistence dp) {
		this.dp = dp;
	}

	private void updateInstance(String seed, String word, SynsetType type) {
		seedArr = database.getSynsets(seed, type);
		wordArr = database.getSynsets(word, type);
	}

	public void sameSenseSimilarity(String seed, String word, Synset synset,
			SynsetType type) {
		if (!seed.equals(word)) {
			updateInstance(seed, word, type);
			double sim = getRelativeFreq(synset, seed, seedArr)
					* getRelativeFreq(synset, word, wordArr);
			System.out.println("Same sense- seed:" + seed + " word :" + word
					+ " type :" + type + " sim:" + sim);
			dp.save(word, type, sim);
		}
	}

	public void hypernymSimilarity(String seed, String word, SynsetType type) {
		if (!seed.equals(word)) {
			System.out.println("Hypernym- seed:" + seed + " word :" + word
					+ " type :" + type);
			updateInstance(seed, word, type);
			double sim = 0;
			for (Synset synset : seedArr) {
				double r = calcHyperSeneSimilarity(seed, word, synset, synset,
						type, 1);
				sim += r;
			}
			System.out.println("Hypernym- seed: " + seed + " word: " + word
					+ " type: " + type + " sim: " + sim);
			dp.save(word, type, sim);
		}
	}

	public void hyponymSimilarity(String seed, String word, SynsetType type) {
		if (!seed.equals(word)) {
			updateInstance(seed, word, type);
			double sim = 0;
			for (Synset synset : seedArr) {
				sim += calcHyponymSeneSimilarity(seed, word, synset, synset,
						type, 1);
			}
			System.out.println("Hyponym- seed:" + seed + " word :" + word
					+ " type :" + type + " sim:" + sim);
			dp.save(word, type, sim);
		}
	}

	private double calcHyponymSeneSimilarity(String seed, String word,
			Synset seedsynet, Synset hyernymSet, SynsetType type, Integer depth) {
		if (type == SynsetType.NOUN) {
			NounSynset nSynset = (NounSynset) hyernymSet;
			NounSynset[] hyponymArr = nSynset.getHyponyms();
			for (NounSynset hypoSet : hyponymArr) {
				if (contains(hypoSet.getWordForms(), word)) {
					return (getRelativeFreq(hypoSet, word, wordArr)
							* (getRelativeFreq(seedsynet, seed, seedArr)) * ((double) 1 / (depth + 1)));
				}
				calcTropoSeneSimilarity(seed, word, seedsynet, hypoSet, type,
						depth + 1);
			}
			depth = depth - 1;
		}
		return 0;
	}

	private double calcHyperSeneSimilarity(String seed, String word,
			Synset seedsynet, Synset hyernymSet, SynsetType type, int depth) {
		if (word.equals("express")) {
			System.out.println();
		}
		if (type == SynsetType.VERB) {
			VerbSynset vSynset = (VerbSynset) hyernymSet;
			VerbSynset[] hypernymSets = vSynset.getHypernyms();
			for (VerbSynset hypernymSet : hypernymSets) {
				if (contains(hypernymSet.getWordForms(), word)) {
					double d = (getRelativeFreq(hypernymSet, word, wordArr)
							* getRelativeFreq(seedsynet, seed, seedArr) * ((double) 1 / (depth + 1)));
					return d;
				}
				return calcHyperSeneSimilarity(seed, word, seedsynet,
						hypernymSet, type, ++depth);
			}
			return 0;
		} else if (type == SynsetType.NOUN) {
			NounSynset nSynset = (NounSynset) hyernymSet;
			NounSynset[] hypernymSets = nSynset.getHypernyms();
			for (NounSynset hypernymSet : hypernymSets) {
				if (contains(hypernymSet.getWordForms(), word)) {
					return (getRelativeFreq(hypernymSet, word, wordArr)
							* (getRelativeFreq(seedsynet, seed, seedArr)) * ((double) 1 / (depth + 1)));
				}
				return calcHyperSeneSimilarity(seed, word, seedsynet,
						hypernymSet, type, ++depth);
			}
			return 0;
		}
		return 0;
	}

	public void troponymSimilarity(String seed, String word, SynsetType type) {
		if (!seed.equals(word)) {
			updateInstance(seed, word, type);
			if (word.equals("repute")) {
				System.out.println();
			}
			double sim = 0;
			for (Synset synset : seedArr) {
				sim += calcTropoSeneSimilarity(seed, word, synset, synset,
						type, 1);
			}
			System.out.println("Troponym- seed:" + seed + " word :" + word
					+ " type :" + type + " sim:" + sim);
			dp.save(word, type, sim);
		}
	}

	public void distSenseSimilarity(String seed, String word, SynsetType type) {
		if (!seed.equals(word)) {
			updateInstance(seed, word, type);
			double sim = 0;
			HashMap<String, Integer> hypernymNodes = new HashMap<String, Integer>();
			for (Synset synset : seedArr) {
				prepareHypenymMap(synset, type, 1, hypernymNodes);
				// System.out.println(hypernymNodes.toString());
				for (Synset wordSynset : wordArr) {
					sim += calDisSimilarity(hypernymNodes, synset, seed,
							wordSynset, wordSynset, word, type, 1);
				}
				hypernymNodes.clear();
			}
			System.out.println("Distinct- seed:" + seed + " word :" + word
					+ " type :" + type + " sim:" + sim + ";;--"
					+ Thread.currentThread().getName());
			dp.save(word, type, sim);
		}
	}

	private double calDisSimilarity(HashMap<String, Integer> hypenymNodes,
			Synset seedSynset, String seed, Synset wordSynset, Synset hypSet,
			String word, SynsetType type, int depth) {
		if (type == SynsetType.VERB) {
			VerbSynset vSynset = (VerbSynset) hypSet;
			VerbSynset[] hypernymArr = vSynset.getHypernyms();
			for (VerbSynset hypernymSet : hypernymArr) {
				if (hypenymNodes.containsKey(hypernymSet.getDefinition())) {
					double calSim = (getRelativeFreq(seedSynset, seed, seedArr)
							* getRelativeFreq(wordSynset, word, wordArr) * ((double) 1 / (depth + 1 + hypenymNodes
							.get(hypernymSet.getDefinition()))));
					return calSim;
				}
				return calDisSimilarity(hypenymNodes, seedSynset, seed,
						wordSynset, hypernymSet, word, type, ++depth);
			}
			return 0;
		} else if (type == SynsetType.NOUN) {
			NounSynset nSynset = (NounSynset) hypSet;
			NounSynset[] hypernymArr = nSynset.getHypernyms();
			for (NounSynset hypernymSet : hypernymArr) {
				if (hypenymNodes.containsKey(hypernymSet.getDefinition())) {

					return (getRelativeFreq(seedSynset, seed, seedArr)
							* getRelativeFreq(wordSynset, word, wordArr) * ((double) 1 / (depth + 1 + hypenymNodes
							.get(hypernymSet.getDefinition()))));
				}
				return calDisSimilarity(hypenymNodes, seedSynset, seed,
						wordSynset, hypernymSet, word, type, ++depth);
			}
			return 0;
		}
		return 0;
	}

	private void prepareHypenymMap(Synset synset, SynsetType type, int depth,
			HashMap<String, Integer> hypernymNodes) {

		if (type == SynsetType.VERB) {
			VerbSynset vSynset = (VerbSynset) synset;
			VerbSynset[] hypernymArr = vSynset.getHypernyms();
			for (VerbSynset hypernymSet : hypernymArr) {
				hypernymNodes.put(hypernymSet.getDefinition(), depth);
				prepareHypenymMap(hypernymSet, type, ++depth, hypernymNodes);
			}
		} else if (type == SynsetType.NOUN) {
			NounSynset nSynset = (NounSynset) synset;
			NounSynset[] hypernymArr = nSynset.getHypernyms();
			for (NounSynset hypernymSet : hypernymArr) {
				hypernymNodes.put(hypernymSet.getDefinition(), depth);
				prepareHypenymMap(hypernymSet, type, ++depth, hypernymNodes);
			}
		}
	}

	private double calcTropoSeneSimilarity(String seed, String word,
			Synset seedsynet, Synset hyernymSet, SynsetType type, Integer depth) {
		if (type == SynsetType.VERB) {
			VerbSynset vSynset = (VerbSynset) hyernymSet;
			VerbSynset[] hypernymSets = vSynset.getTroponyms();
			for (VerbSynset verbSynset : hypernymSets) {
				if (contains(verbSynset.getWordForms(), word)) {
					return (getRelativeFreq(verbSynset, word, wordArr)
							* (getRelativeFreq(seedsynet, seed, seedArr)) * ((double) 1 / (depth + 1)));
				}
				calcTropoSeneSimilarity(seed, word, seedsynet, verbSynset,
						type, depth + 1);
			}
			depth = depth - 1;
		}
		return 0;
	}

	private boolean contains(String[] arr, String word) {
		for (String arrWord : arr) {
			if (arrWord.equals(word))
				return true;
		}
		return false;
	}

	private boolean compare(Synset synset1, Synset synset2) {
		return synset1.getDefinition().equals(synset2.getDefinition());
	}

	private double getRelativeFreq(Synset synset, String word,
			Synset[] synsetArr) {
		int givenSynsetFreq = 0;
		int totalFreq = 0;
		for (Synset synset2 : synsetArr) {
			if (word.equals("humanities")) {
				System.out.println();
			}
			try {
				totalFreq += synset2.getTagCount(word) == 0 ? 1 : synset2
						.getTagCount(word);
			} catch (Exception ex) {
			}
			if (compare(synset, synset2)) {
				try {
					givenSynsetFreq = synset2.getTagCount(word) == 0 ? 1
							: synset2.getTagCount(word);
				} catch (Exception ex) {
				}
			}
		}
		double s = ((double) givenSynsetFreq / totalFreq);
		return s;
	}
}
