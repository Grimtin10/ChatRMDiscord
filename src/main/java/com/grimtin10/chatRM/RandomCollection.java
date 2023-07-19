package com.grimtin10.chatRM;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class RandomCollection<E> {
	private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
	private final Random random;
	private double total = 0;

	public RandomCollection() {
		this(new Random());
	}

	public RandomCollection(Random random) {
		this.random = random;
	}

	public RandomCollection<E> add(double weight, E result) {
		if (weight <= 0)
			return this;
		total += weight;
		map.put(total, result);
		return this;
	}

	public E next() {
		double value = random.nextDouble() * total;
		Entry<Double, E> selected = map.higherEntry(value);
		if (selected == null)
			return null;
		return selected.getValue();
	}

	public void clear() {
		total = 0;
		map.clear();
	}
}