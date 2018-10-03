package ru.mail.polis.turenkoaa;

import lombok.SneakyThrows;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

public class KVEntityDao implements KVDao {

    private final DB db;

    @SneakyThrows
    public KVEntityDao(@NotNull final File data) {
        Options options = new Options();
        options.createIfMissing(true);
        db = Iq80DBFactory.factory.open(data, options);
    }

    @NotNull
    @Override
    public byte[] get(@NotNull byte[] key) throws NoSuchElementException, IOException {
        byte[] value = db.get(key);
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;

    }

    @Override
    public void upsert(@NotNull byte[] key, @NotNull byte[] value) throws IOException {
        db.put(key, value);
    }

    @Override
    public void remove(@NotNull byte[] key) throws IOException {
        db.delete(key);
    }

    @Override
    public void close() throws IOException {
        db.close();
    }
}
