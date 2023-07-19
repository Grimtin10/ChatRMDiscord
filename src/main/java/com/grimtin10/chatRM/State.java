package com.grimtin10.chatRM;

public class State {
	public Token[] prev;
	public Token next;

	public int count = 0;

	public State(Token[] prev, Token next) {
		this.prev = new Token[prev.length];
		for (int i = 0; i < prev.length; i++) {
			this.prev[i] = prev[i];
		}
		this.next = next;
	}

	public int hashCode() {
		int prevHash = 0;

		for (int i = 0; i < prev.length; i++) {
			prevHash += prev[i].hashCode();
		}

		return prevHash + (next.hashCode() >> 2);
	}

	public boolean equals(Object o) {
		if (!(o instanceof State))
			return false;

		return ((State) o).hashCode() == hashCode();
	}

	public String toString() {
		String s = "";

		for (int i = 0; i < prev.length; i++) {
			s += prev[i] + " ";
		}

		s += ": " + next;

		return s;
	}

	public int match(Token[] prev) {
		for (int i = 0; i < this.prev.length; i++) {
			boolean match = true;
			for (int j = i; j < this.prev.length; j++) {
				if (this.prev[j] != null && prev[j] != null) {
					if (!this.prev[j].equals(prev[j])) {
						match = false;
						break;
					}
				}
			}
			if (match)
				return this.prev.length - i;
		}
		return 0;
	}
}
