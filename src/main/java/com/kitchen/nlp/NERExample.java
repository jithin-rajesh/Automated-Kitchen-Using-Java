package com.kitchen.nlp;

import java.util.*;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class NERExample {
    public static void main(String[] args) {
        
        StanfordCoreNLP stanfordCoreNLP = Pipeline.getPipeline();

        String text = "Start by Washing the Basmati Rice";

        CoreDocument coreDocument = new CoreDocument(text);

        stanfordCoreNLP.annotate(coreDocument);

        List<CoreLabel> coreLabels = coreDocument.tokens();

        for(CoreLabel coreLabel : coreLabels) {
            String ner = coreLabel.get(CoreAnnotations.NamedEntityTagAnnotation.class);

            System.out.println(coreLabel.originalText() + " = " + ner);
        }
    }
}
