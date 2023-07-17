package com.grimtin10.chatRM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.grimtin10.chatRMBot.App;

public class MarkovChain {
  public int size = 1;

  HashSet<State> brain = new HashSet<State>();

  // last prediction data
  ArrayList<Token> botResult = new ArrayList<Token>();
  int predIndex = 0;
  int offset = 0;

  public MarkovChain() {
  }

  public void train(String[] data) {
    brain.clear();
    
    ArrayList<Token> tokens = new ArrayList<Token>();
    for (int i = 0; i < data.length; i++) {
      Token[] lineTokens = App.tokenize(data[i]);

      for (int j = 0; j < lineTokens.length; j++) {
        tokens.add(lineTokens[j]);
      }
      tokens.add(new Token(" "));
    }

    size = Math.max(1, tokens.size() / 10000);
    
    System.out.println("CURRENT BOT LEVEL: " + size + " (" + tokens.size() + "/" + (size + 1) * 10000 + ")");
    
    HashMap<State, Integer> counts = new HashMap<State, Integer>();

    Token[] prev = new Token[size];
    for (int i = 0; i < tokens.size() - size; i++) {
      if (!tokens.get(i + size).value.contains(":") && !tokens.get(i + size).value.contains("HUMAN") && !tokens.get(i + size).value.contains("BOT")) {
        for (int j = 0; j < size; j++) {
          prev[j] = tokens.get(i + j);
        }

        State state = new State(prev, tokens.get(i + size));
        if (counts.containsKey(state)) {
          counts.replace(state, counts.get(state) + 1);
        } else {
          counts.put(state, 1);
        }
      }
    }

    for (Entry<State, Integer> e : counts.entrySet()) {
      e.getKey().count = e.getValue();
      brain.add(e.getKey());
    }
  }

  public String predict(String in, int len) {
    String s = "";

    botResult.clear();

    ArrayList<Token> tokens = new ArrayList<Token>();
    Token[] tokenized = App.tokenize(in);
    for (int i = 0; i < tokenized.length; i++) {
      tokens.add(tokenized[i]);
      botResult.add(tokenized[i]);
    }

    Token[] prev = new Token[size];

    predIndex = Math.max(tokens.size() - size, 0);

    int tokenCount = 0;
    
    offset = Math.max(size - tokens.size(), 0);

    RandomCollection<Token> nextTokens = new RandomCollection<Token>();
    while (true) {
      nextTokens.clear();

      int index = Math.max(tokens.size() - size, 0);
      int offset = Math.max(size - tokens.size(), 0);
      for (int j = 0; j < Math.min(size, tokens.size()); j++) {
        prev[j + offset] = tokens.get(index + j);
      }

      //println();
      for (State state : brain) {
        float amnt = (state.match(prev) / (float) size) * 2  ;
        if (amnt > 0) {
          nextTokens.add(state.count * amnt * amnt, state.next);
        }/* else {
         print("PREV: ");
         for (int i=0; i<prev.length; i++) {
         print(prev[i] + " ");
         }
         println();
         print("STATE: ");
         println(state);
         }*/
      }

      Token nextToken = nextTokens.next();
      if (nextToken == null) break;

      botResult.add(nextToken);

      String value = nextToken.value;
      s += value;
      tokens.add(nextToken);
      tokenCount++;

      if (tokenCount >= len && (value.contains(".") || value.contains("!") || value.contains("?"))) {
        break;
      }
    }

    return s;
  }
}
