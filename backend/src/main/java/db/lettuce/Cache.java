package db.lettuce;

public interface Cache extends AutoCloseable {
    @Override
    default void close() {
    }
}
