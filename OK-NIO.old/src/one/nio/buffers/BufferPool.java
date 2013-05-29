package one.nio.buffers;

public interface BufferPool<T> {
	T getBuffer(int size);
	void returnBuffer(T buffer);
}
