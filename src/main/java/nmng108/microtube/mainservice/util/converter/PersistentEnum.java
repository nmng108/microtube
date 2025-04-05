package nmng108.microtube.mainservice.util.converter;

public interface PersistentEnum<P> extends EnumMatcher<P>, PersistedValueGetter<P> {
    @Override
    default boolean matches(P value) {
        return getPersistedValue().equals(value);
    }
}
