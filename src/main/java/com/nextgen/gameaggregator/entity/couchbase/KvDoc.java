package com.nextgen.gameaggregator.entity.couchbase;

/**
 * Generic Couchbase KV document wrapper.
 *
 * <p>This class separates persistence concerns (document id, CAS token)
 * from your pure domain objects. The payload {@code T} is what gets
 * serialized to/from Couchbase as JSON, while {@code id} and {@code cas}
 * come from Couchbase metadata.</p>
 *
 * @param <T> The domain type being persisted.
 */
public final class KvDoc<T> {

    /**
     * Couchbase document key.
     * Not stored inside the JSON body; managed by Couchbase.
     */
    private String id;

    /**
     * CAS (Compare-And-Swap) token from Couchbase.
     * Used for optimistic concurrency control.
     */
    private long cas;

    /**
     * The actual domain object payload to persist as JSON.
     */
    private T payload;

    // --- Constructors ---

    /** Empty constructor (needed for deserialization frameworks). */
    public KvDoc() {}

    public KvDoc(String id, long cas, T payload) {
        this.id = id;
        this.cas = cas;
        this.payload = payload;
    }

    /**
     * Factory method for new documents where CAS is not yet known.
     */
    public static <T> KvDoc<T> of(String id, T payload) {
        return new KvDoc<>(id, 0L, payload);
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCas() {
        return cas;
    }

    public void setCas(long cas) {
        this.cas = cas;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    // --- Utility ---

    @Override
    public String toString() {
        return "KvDoc{" +
                "id='" + id + '\'' +
                ", cas=" + cas +
                ", payload=" + payload +
                '}';
    }
}
