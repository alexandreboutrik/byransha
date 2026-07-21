package byransha.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class Q<E> implements Iterable<E> {
	private final BlockingQueue<E> q;
	public QListener<E> listener;
	private Thread thread;

	public Q(int capacity) {
		this.q = new ArrayBlockingQueue<>(capacity);
	}

	public int size() {
		return q.size();
	}

	public E poll_async() {
		return q.poll();
	}

	public E peek() {
		return q.peek();
	}

	public E poll_sync() {
		try {
			return q.take();
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	public E poll_sync(double timeout) {
		try {
			long timeOutNS = (long) (timeout * 1000000000);
			E e = q.poll(timeOutNS, TimeUnit.NANOSECONDS);
			return e;
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	public E pollOrFail_sync(double timeout) {
		E e = poll_sync(timeout);

		if (e != null) {
			return e;
		}

		throw new TimeoutException();
	}

	public void add_sync(E in) {
		try {
			q.put(in);

			if (listener != null) {
				listener.newElement(this, in);
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	public Thread onMsg(Consumer<E> l, BooleanSupplier runCondition) {
		if (thread != null)
			throw new IllegalStateException("thread already exists");

		thread = new Thread(() -> {
			while (runCondition.getAsBoolean()) {
				E msg = poll_sync();
				l.accept(msg);
			}
		});

		thread.start();
		return thread;
	}

	public void cancelEventisation() {
		if (thread != null) {
			thread.interrupt();
		}
	}

	@Override
	public Iterator<E> iterator() {
		return q.iterator();
	}

	public List<E> toList() {
		var r = new ArrayList<E>(size());
		forEach(m -> r.add(m));
		return r;
	}

	@Override
	public String toString() {
		return q.toString();
	}

}
