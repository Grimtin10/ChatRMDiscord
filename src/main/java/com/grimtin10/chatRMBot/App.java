package com.grimtin10.chatRMBot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;

import com.grimtin10.chatRM.MarkovChain;
import com.grimtin10.chatRM.Token;

public class App {
	static HashSet<Token> tokens;

	static MarkovChain brain;

	public static void main(String[] args) {
		tokens = new HashSet<Token>();

		tokens.add(new Token("HUMAN: "));
		tokens.add(new Token("BOT: "));
		tokens.add(new Token("HUMAN:"));
		tokens.add(new Token("BOT:"));
		tokens.add(new Token("HUMAN"));
		tokens.add(new Token("BOT"));
		
		System.out.println("loading tokens...");
		String[] file = loadStrings("tokens.txt");
		for (int i = 0; i < file.length; i++) {
			if (!file[i].contains("HUMAN") && !file[i].contains("BOT")) {
				tokens.add(new Token(file[i]));
			}
		}
		
		brain = new MarkovChain();
		brain.train(loadStrings("data.txt"));

		String token = "!!!!BOT TOKEN!!!!";

		DiscordApi api = new DiscordApiBuilder().setToken(token).addIntents(Intent.MESSAGE_CONTENT).login().join();

		// Add a listener which answers with "Pong!" if someone writes "!ping"
		api.addMessageCreateListener(event -> {
			if (event.getMessageContent().equalsIgnoreCase("!help")) {
				event.getChannel().sendMessage("No.");
			}
			if (event.getChannel().getId() == 1130286372272492574L) {
				if (event.getMessage().getAuthor().getId() != 1130283529251594362L) {
					event.getChannel().sendMessage(predict(event.getMessage().getContent()));
				}
			}

		});

		// Print the invite url of your bot
		System.out.println("You can invite the bot by using the following url: " + api.createBotInvite());
	}
	
	private static String predict(String text) {
		String predicted = "";
		
		text = text.replace('\n', ' ');
		
	    try {
	      if(brain.size > 5) {
	        predicted = brain.predict("HUMAN: " + text + " BOT: ", (int)Math.floor(random(30)));
	        Files.write(Paths.get("data.txt"), ("HUMAN: " + text + "\nBOT: " + predicted + "\n").getBytes(), StandardOpenOption.APPEND);
	      } else if (brain.size > 3) {
	        predicted = brain.predict("HUMAN: " + text + " HUMAN: ", (int)Math.floor(random(30)));
	        Files.write(Paths.get("data.txt"), ("HUMAN: " + text + "\nBOT: " + predicted + "\n").getBytes(), StandardOpenOption.APPEND);
	      } else {
	        predicted = brain.predict("HUMAN: " + text + " HUMAN: ", (int)Math.floor(random(30)));
	        Files.write(Paths.get("data.txt"), ("HUMAN: " + text + "\n").getBytes(), StandardOpenOption.APPEND);
	      }
	    }
	    catch (IOException e) {
	      e.printStackTrace();
	    }
		brain.train(loadStrings("data.txt"));
	    
	    return predicted;
	}
	
	public static double random(float max) {
		return Math.random() * max;
	}
	
	// Tokenize a given string.
	public static Token[] tokenize(String in) {
		ArrayList<Token> result = new ArrayList<Token>(); // Result tokens

		// Make every character a token
		for (int i = 0; i < in.length(); i++) {
			result.add(new Token(in.charAt(i) + ""));
		}

		// Iterate over the string until every token has been found
		while (true) {
			boolean changedToken = false;
			for (int i = 0; i < result.size() - 1; i++) {
				Token token = new Token(result.get(i), result.get(i + 1));

				int offset = 2;
				while (i + offset < result.size()) {
					if (!tokens.contains(new Token(token, result.get(i + offset)))) {
						break;
					}
					token = new Token(token, result.get(i + offset));
					offset++;
				}

				if (tokens.contains(token)) {
					result.set(i, token);
					for (int j = 0; j < offset - 1; j++) {
						result.remove(i + 1);
					}
					changedToken = true;
				}
			}

			if (!changedToken)
				break; // Stop if no tokens were found
		}

		// Return the result
		return result.toArray(new Token[result.size()]);
	}

	public static String[] loadStrings(String file) {
		BufferedReader reader;

		ArrayList<String> lines = new ArrayList<String>();
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();

			while (line != null) {
				lines.add(line);
				line = reader.readLine();
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return lines.toArray(new String[lines.size()]);
	}
}
