package me.kaigermany.ultimateutils.sync.thread;

@FunctionalInterface
public interface CoProcessSupplier <T> {
	public T get() throws Exception;
}
