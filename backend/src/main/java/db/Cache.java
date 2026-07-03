package db;

public interface Cache extends AutoCloseable {
    @Override
    default void close() {
    }
}
