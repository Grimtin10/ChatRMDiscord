package com.grimtin10.chatRMBot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;

import com.grimtin10.chatRM.MarkovChain;
import com.grimtin10.chatRM.Token;

public class App {
	static HashSet<Token> tokens;

	static HashMap<Long, ArrayList<String>> messages;

	static MarkovChain brain;
	static MarkovChain userBrain;

	public static DiscordApi api;

	static String ASCII = " !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

	public static String[] blacklist;

	public static int blacklistCount = 0;

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		tokens = new HashSet<Token>();

		tokens.add(new Token("HUMAN: "));
		tokens.add(new Token("BOT: "));
		tokens.add(new Token("HUMAN:"));
		tokens.add(new Token("BOT:"));
		tokens.add(new Token("HUMAN"));
		tokens.add(new Token("BOT"));
		tokens.add(new Token(".gif"));
		tokens.add(new Token(".GIF"));
		tokens.add(new Token("didnt"));

		for (int i = 0; i < ASCII.length(); i++) {
			tokens.add(new Token(ASCII.charAt(i) + ""));
		}

		System.out.println("loading tokens...");
		String[] file = loadStrings("tokens.txt");
		for (int i = 0; i < file.length; i++) {
			if (!file[i].contains("HUMAN") && !file[i].contains("BOT") && !file[i].startsWith(":")) {
				tokens.add(new Token(file[i]));
			}
		}

		blacklist = loadStrings("wordblacklist.txt");

		brain = new MarkovChain(true);
		brain.train(loadStrings("data.txt"));

		String token = "[YOUR BOT TOKEN]";

		api = new DiscordApiBuilder().setToken(token).addIntents(Intent.MESSAGE_CONTENT, Intent.DIRECT_MESSAGES, Intent.DIRECT_MESSAGE_TYPING).login().join();

		File f = new File("messages.txt");
		if (f.exists()) {
			try {
				FileInputStream in = new FileInputStream(f);
				ObjectInputStream oIn = new ObjectInputStream(in);

				messages = (HashMap<Long, ArrayList<String>>) oIn.readObject();

				in.close();
				oIn.close();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			try {
				f.createNewFile();
				ServerTextChannel channel = api.getChannelById(1130594381682982922L).get().asServerTextChannel().get();
				channel.sendMessage("Collecting all messages...");

				messages = new HashMap<Long, ArrayList<String>>();

				for (TextChannel textChannel : api.getTextChannels()) {
					try {
						System.out.println(textChannel.asServerTextChannel().get().getName());
						List<Message> channelMessages = textChannel.getMessagesAsStream().collect(Collectors.toList());
						for (Message message : channelMessages) {
							if (!messages.containsKey(message.getAuthor().getId())) {
								messages.put(message.getAuthor().getId(), new ArrayList<String>());
							}
							messages.get(message.getAuthor().getId()).add(message.getContent().replaceAll("[^ -~]", "").replace('\n', ' '));
						}
						channel.sendMessage("Collected all messages in " + textChannel.asServerTextChannel().get().getName());
					} catch (Exception e) {
						System.err.println("Failed to read channel. (e: " + e.getMessage() + ")");
					}
				}
				channel.sendMessage("Saving messages...");

				FileOutputStream out = new FileOutputStream(f);
				ObjectOutputStream oOut = new ObjectOutputStream(out);

				oOut.writeObject(messages);

				oOut.close();
				out.close();

				channel.sendMessage("Done.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Add a listener which answers with "Pong!" if someone writes "!ping"
		api.addMessageCreateListener(event -> {
			if (event.getMessageContent().equalsIgnoreCase("!help")) {
				event.getChannel().sendMessage("!help for help\n!reply to reply to the last message\n!continue to continue text\n!tokenize debug command that could look cool");
			} else if (event.getMessageContent().toLowerCase().startsWith("!talklike")) {
				(new Thread() {
					public void run() {
						long id = -1;

						if (event.getMessage().getMentionedUsers().size() > 0) {
							User mentioned = event.getMessage().getMentionedUsers().get(0);
							id = mentioned.getId();
						} else if (event.getMessageContent().contains("<@")) {
							String idStr = event.getMessageContent().split("<@")[1];
							idStr = idStr.substring(0, idStr.length() - 1);
							id = Long.parseLong(idStr);
						}

						if (id != -1) {
							ArrayList<String> userMessages = messages.get(id);
							userBrain = new MarkovChain(false);
							userBrain.train(userMessages.toArray(new String[userMessages.size()]));

							String predict = "";
							int attempts = 0;
							while (predict.trim().length() == 0 && attempts < 1000) {
								String firstWord = userMessages.get((int) Math.floor(random(userMessages.size() - 0.001f))).split(" ")[0];
								predict = userBrain.predict(firstWord + " ", (int) Math.floor(random(30)));
//							System.out.println(firstWord);
								attempts++;
							}

							if (attempts >= 1000) {
								event.getChannel().sendMessage("hit attempt limit");
							}

							event.getChannel().sendMessage("**(lvl " + userBrain.size + ")**\n" + predict);
						}
					}
				}).start();
			} else if (event.getMessageContent().toLowerCase().startsWith("!updatelist")) {
				blacklist = loadStrings("wordblacklist.txt");
			} else if (event.getMessageContent().toLowerCase().startsWith("!getcount")) {
				event.getChannel().sendMessage(blacklistCount + " blacklisted lines.");
			} else if (event.getMessageContent().toLowerCase().startsWith("!getlines")) {
				if (event.getMessage().getAuthor().getId() == 358736445361487872L) {
					String[] data = loadStrings("data.txt");
					for (int i = 0; i < data.length; i++) {
						if (isBlacklisted(data[i])) {
							System.out.println(data[i]);
						}
					}
				} else {
					event.getChannel().sendMessage("no");
				}
			} else if (event.getMessageContent().toLowerCase().startsWith("!continue")) {
				brain.train(loadStrings("data.txt"));
				String content = event.getMessageContent();
				String arg = content.substring(content.indexOf(" ") + 1);
				String prediction = brain.predict(arg + " ", (int) Math.floor(random(100)));

				int attempts = 0;
				while (prediction.trim().length() == 0 && attempts < 1000) {
					prediction = brain.predict(arg, (int) Math.floor(random(100)));
				}

				if (attempts >= 1000) {
					event.getChannel().sendMessage("hit attempt limit");
				}

				event.getChannel().sendMessage("**" + arg + "** " + prediction);
			} else if (event.getMessageContent().toLowerCase().startsWith("!reply")) {
				try {
					Message lastMessage = event.getChannel().getMessagesBefore(1, event.getMessageId()).get().iterator().next();

					lastMessage.reply(predict(lastMessage.getContent(), lastMessage.getAuthor().getId()));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (event.getMessageContent().toLowerCase().startsWith("!tokenize")) {
				try {
					Message lastMessage = event.getChannel().getMessagesBefore(1, event.getMessageId()).get().iterator().next();

					String tokenized = "";
					Token[] tokens = tokenize(lastMessage.getContent());
					for (int i = 0; i < tokens.length; i++) {
						tokenized += tokens[i].toString();
					}

					event.getChannel().sendMessage("```" + tokenized + "```");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (event.getMessageContent().toLowerCase().startsWith("!dmuser")) {
				String[] cmdArgs = event.getMessageContent().split(" ");
				User user;
				try {
					user = api.getUserById(Long.parseLong(cmdArgs[1])).get();
					if (user != null) {
						user.sendMessage("hello");
					} else {
						System.err.println("Failed to get user " + Long.parseLong(cmdArgs[1]));
					}
				} catch (NumberFormatException | InterruptedException | ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (event.getMessageContent().toLowerCase().startsWith("!sendchannel")) {
				String[] cmdArgs = event.getMessageContent().split(" ");
				Channel channel;
				try {
					channel = api.getChannelById(Long.parseLong(cmdArgs[1])).get();
					if (channel != null) {
						channel.asPrivateChannel().get().sendMessage("hello");
					} else {
						System.err.println("Failed to get channel " + Long.parseLong(cmdArgs[1]));
					}
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchElementException e) {
					System.err.println("Failed to get channel " + Long.parseLong(cmdArgs[1]));
				}
			} else {
				if (event.getMessage().getMentionedUsers().size() > 0) {
					User mentioned = event.getMessage().getMentionedUsers().get(0);
					if (mentioned.getId() == 1130283529251594362L) {
						if (event.getMessage().getContent().toLowerCase().contains("reply")) {
							try {
								Message lastMessage = event.getChannel().getMessagesBefore(1, event.getMessageId()).get().iterator().next();

								lastMessage.reply(predict(lastMessage.getContent(), lastMessage.getAuthor().getId()));
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (ExecutionException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
				Message message = event.getMessage();
				ArrayList<String> list = messages.get(message.getAuthor().getId());
				if (list != null) {
					list.add(message.getContent());
				} else {
					messages.put(message.getAuthor().getId(), new ArrayList<String>());
					list = messages.get(message.getAuthor().getId());
					list.add(message.getContent());

					try {
						FileOutputStream out = new FileOutputStream(f);
						ObjectOutputStream oOut = new ObjectOutputStream(out);

						oOut.writeObject(messages);

						oOut.close();
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (event.getMessageContent().toLowerCase().contains("big fat dingle"))
					return;
				if (event.getChannel().getId() == 1130286372272492574L) {
					if (event.getMessage().getAuthor().getId() != 1130283529251594362L) {
						event.getChannel().type();
						try {
							Thread.sleep(400);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						event.getChannel().sendMessage(predict(event.getMessage().getContent(), event.getMessage().getAuthor().getId()));
					}
				} else {
					if (event.getMessage().getAuthor().getId() != 1130283529251594362L) {
						if (event.getChannel().getType() == ChannelType.PRIVATE_CHANNEL) {
							PrivateChannel channel = event.getChannel().asPrivateChannel().get();
							if (channel != null) {
								event.getChannel().type();
								try {
									Thread.sleep(700);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								System.out.println(channel.getRecipient().get().getName() + ": " + event.getMessage().getContent() + " (id: " + channel.getRecipientId().get() + ")");
								String response = predict(event.getMessage().getContent(), event.getMessage().getAuthor().getId());
								System.out.println("CHATRM: " + response);
								event.getChannel().sendMessage(response);
							}
						}
					}
				}
			}
		});

		// Print the invite url of your bot
		System.out.println("You can invite the bot by using the following url: " + api.createBotInvite());
	}

	public static boolean isBlacklisted(String text) {
		for (int i = 0; i < blacklist.length; i++) {
			if (text.toLowerCase().contains(blacklist[i])) {
				return true;
			}
		}
		return false;
	}

	private static String predict(String text, long userID) {
		String predicted = "";

		text = text.replace('\n', ' ');
		text = text.replaceAll("[^ -~]", "");

		brain.train(loadStrings("data.txt"));
		if (userID != 1081004946872352958L) {
			try {
				if (brain.size > 7) {
					predicted = brain.predict("HUMAN: " + text + " BOT: ", (int) Math.floor(random(30)));
					Files.write(Paths.get("data.txt"), ("HUMAN: " + text + "\nBOT: " + predicted + "\n").getBytes(), StandardOpenOption.APPEND);
				} else if (brain.size > 3) {
					predicted = brain.predict("HUMAN: " + text + " HUMAN: ", (int) Math.floor(random(30)));
					Files.write(Paths.get("data.txt"), ("HUMAN: " + text + "\nBOT: " + predicted + "\n").getBytes(), StandardOpenOption.APPEND);
				} else {
					predicted = brain.predict("HUMAN: " + text + " HUMAN: ", (int) Math.floor(random(30)));
					Files.write(Paths.get("data.txt"), ("HUMAN: " + text + "\n").getBytes(), StandardOpenOption.APPEND);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		api.updateActivity(ActivityType.PLAYING, "Bot Level: " + brain.size);

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
				Token workingToken = new Token(result.get(i), result.get(i + 1));

				int workingOffset = 2;
				int offset = 2;
				while (i + workingOffset < result.size()) {
					workingToken = new Token(workingToken, result.get(i + workingOffset));
					if (tokens.contains(workingToken)) {
						token = new Token(workingToken.value);
						offset = workingOffset + 1;
					}
					if (result.get(i + workingOffset).value.equals(" ")) {
						break;
					}
					workingOffset++;
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
