package com.grimtin10.chatRM;

public class Token {
	public String value = "";

	public Token(Token t1, Token t2) {
		value = t1.value + t2.value;
	}

	public Token(String value) {
		this.value = value;
	}

	public int hashCode() {
		return value.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof Token))
			return false;

		return ((Token) o).value.equals(value);
	}

	public int indexOf(String in) {
		if (!contains(in))
			return -1;

		for (int i = 0; i < in.length(); i++) {
			if (in.substring(i).startsWith(value)) {
				return i;
			}
		}
		return -1;
	}

	public boolean contains(String in) {
		return in.contains(value);
	}

	public String toString() {
		return "[" + value + "]";
	}
}