package me.kaigermany.ultimateutils.sync.thread;

import java.util.Iterator;

public interface FiniteIterator<T> extends Iterator<T> {
	int getSize();
}
