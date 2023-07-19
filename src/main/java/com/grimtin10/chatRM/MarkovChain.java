package com.grimtin10.chatRM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.javacord.api.entity.channel.TextChannel;

import com.grimtin10.chatRMBot.App;

public class MarkovChain {
	public int size = 3;

	HashSet<State> brain = new HashSet<State>();

	boolean isMain = false;

	public MarkovChain(boolean isMain) {
		this.isMain = isMain;
	}

	public void train(String[] data) {
		long startMS = System.currentTimeMillis();

		brain.clear();

		if (isMain) {
			App.blacklistCount = 0;
		}

		ArrayList<Token> tokens = new ArrayList<Token>();
		for (int i = 0; i < data.length; i++) {
			if (data[i].length() < 300 && !App.isBlacklisted(data[i]) && !data[i].trim().equals("BOT:")) {
				Token[] lineTokens = App.tokenize(data[i]);

				for (int j = 0; j < lineTokens.length; j++) {
					tokens.add(lineTokens[j]);
				}
				tokens.add(new Token(" "));
			} else if (App.isBlacklisted(data[i])) {
				App.blacklistCount++;
			}
		}

		if (Math.max(1, tokens.size() / 7500) > size && isMain) {
			if (App.api != null) {
				((TextChannel) App.api.getChannelById(1130286372272492574L).get()).sendMessage("@here THE BOT HAS LEVELED UP!");
			}
		}
		size = Math.max(1, tokens.size() / 7500);

		if (isMain) {
			System.out.print("CURRENT BOT LEVEL: " + size + " (" + tokens.size() + "/" + (size + 1) * 7500 + ")");
		} else {
			System.out.print("user bot level: " + size + " (" + tokens.size() + "/" + (size + 1) * 7500 + ")");
		}

		HashMap<State, Integer> counts = new HashMap<State, Integer>();

		Token[] prev = new Token[size];
		for (int i = 0; i < tokens.size() - size; i++) {
			if (!tokens.get(i + size).value.contains("HUMAN") && !tokens.get(i + size).value.contains("BOT")) {
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

		System.out.println(" (" + (System.currentTimeMillis() - startMS) + " ms)");
	}

	public String predict(String in, int len) {
		String s = "";

		ArrayList<Token> tokens = new ArrayList<Token>();
		Token[] tokenized = App.tokenize(in);
		for (int i = 0; i < tokenized.length; i++) {
			tokens.add(tokenized[i]);
		}

		Token[] prev = new Token[size];

		int tokenCount = 0;

		Token nextToken = null;

		RandomCollection<Token> nextTokens = new RandomCollection<Token>();
		while (true) {
			nextTokens.clear();

			int index = Math.max(tokens.size() - size, 0);
			int offset = Math.max(size - tokens.size(), 0);
			for (int j = 0; j < Math.min(size, tokens.size()); j++) {
				prev[j + offset] = tokens.get(index + j);
			}

//			System.out.println();
			for (State state : brain) {
				float amnt = (state.match(prev) / (float) size) * 3;
				if (amnt > 0) {
					nextTokens.add(state.count * amnt * amnt, state.next);
				}
//					  else { System.out.print("PREV: "); for (int i = 0; i < prev.length; i++) {
//					  System.out.print(prev[i] + " "); } System.out.println();
//					  System.out.print("STATE: "); System.out.println(state); }

			}

			boolean lastSpace = false;
			if (nextToken != null && nextToken.value.trim().length() == 0)
				lastSpace = true;

			nextToken = nextTokens.next();
			if (nextToken == null)
				break;

			if (lastSpace && nextToken.value.trim().length() == 0)
				break;

			String value = nextToken.value;
			s += value;
			tokens.add(nextToken);
			tokenCount++;

			if (tokenCount >= len && (value.contains(".") || value.contains("!") || value.contains("?"))) {
				// break;
			}
			// break;
		}

		return s;
	}
}
