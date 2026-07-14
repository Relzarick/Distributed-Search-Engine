package tokenizer;

import java.util.Set;

public class BaseTokenization {
    protected static final Set<String> STOP_WORDS = Set.of(
            // Articles
            "a", "an", "the",

            // Personal Pronouns
            "i", "me", "my", "mine", "myself",
            "you", "your", "yours", "yourself", "yourselves",
            "he", "him", "his", "himself",
            "she", "her", "hers", "herself",
            "it", "its", "itself",
            "we", "us", "our", "ours", "ourselves",
            "they", "them", "their", "theirs", "themselves",
            "thee", "thine", "thy", "thyself", "oneself",

            // Relative Pronouns
            "who", "whom", "whose", "which", "what",
            "whichever", "whilst", "whoever", "whomever",
            "whoso", "whosoever", "whatever", "whatsoever",

            // Demonstratives
            "this", "that", "these", "those",

            // Coordinating Conjunctions
            "and", "but", "or", "nor", "for", "yet", "so",

            // Subordinating Conjunctions
            "although", "because", "since", "unless", "until",
            "while", "whereas", "whether", "though", "even",
            "if", "as", "than", "albeit",

            // Prepositions
            "about", "above", "across", "after", "against", "along",
            "among", "around", "at", "before", "behind", "below",
            "beneath", "beside", "besides", "between", "beyond",
            "by", "despite", "down", "during", "except", "from",
            "in", "inside", "instead", "into", "near",
            "of", "off", "on", "onto", "out", "outside", "over",
            "past", "per", "regarding", "through", "throughout",
            "to", "toward", "towards", "under", "underneath",
            "up", "upon", "via", "with", "within", "without",
            "alongside", "amid", "amidst", "amongst", "apart",
            "aside", "aslant", "astride", "astraddle", "athwart",
            "atop", "atween", "afore", "anent", "abaft",
            "concerning", "circa", "downward", "downwards",
            "excepting", "inward", "inwards", "neath", "nethermost",
            "nigh", "outwith", "overall", "unto",
            "versus", "vis-a-vis", "withal", "whereinto", "wherefrom",

            // Auxiliary Verbs
            "am", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "having",
            "do", "does", "did", "doing",
            "will", "would", "shall", "should",
            "may", "might", "must", "can", "could",
            "need", "ought", "cannot", "doth", "hast", "hath",

            // Negations
            "no", "not", "never", "neither", "none",
            "nobody", "nothing", "nowhere", "hardly", "scarcely", "barely",
            "ain't", "aren't", "can't", "couldn't", "daren't", "didn't",
            "doesn't", "don't", "hadn't", "hasn't", "haven't", "isn't", "mayn't",
            "mightn't", "mustn't", "needn't", "notwithstanding",
            "oughtn't", "shan't", "shouldn't", "wasn't",
            "weren't", "won't", "wouldn't",

            // Basic Context-Light Adverbs
            "again", "almost", "already", "also", "always",
            "anyway", "away", "back", "else", "enough",
            "far", "here", "however", "just",
            "merely", "mostly", "nearly", "now", "often",
            "once", "only", "otherwise", "perhaps",
            "quite", "rather", "really", "simply",
            "sometimes", "still", "then", "there",
            "therefore", "thus", "too", "very", "well",

            // Basic Determiners
            "all", "any", "another", "both", "each", "either",
            "every", "few", "less", "little", "many",
            "more", "most", "much", "next", "other",
            "own", "same", "several", "some", "such",
            "various", "whole",

            // Interrogatives
            "how", "when", "where", "why",
            "how'd", "how'll", "how's",
            "what'll", "what've", "what'd", "whence",
            "whenever", "when's", "when'll",
            "where's", "where'd", "where'll", "who'd", "who'll",
            "why'd", "why'll", "why's",

            // Basic Contractions
            "he'd", "he'll", "he's",
            "i'd", "i'll", "i'm", "i've", "it'd", "it'll", "it's",
            "let's", "she'd", "she'll", "she's",
            "that's", "there's", "they'd", "they'll",
            "they're", "they've", "we'd", "we're", "we've",
            "what's", "who's",
            "you'd", "you'll", "you're", "you've",
            "could've", "might've", "must've", "should've",
            "that'll", "that've", "there'd", "there'll", "there're", "there've",
            "'tis", "'twas", "would've"
    );

}