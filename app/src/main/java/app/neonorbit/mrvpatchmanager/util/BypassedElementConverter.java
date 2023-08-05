package app.neonorbit.mrvpatchmanager.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jsoup.nodes.Element;

import pl.droidsonroids.jspoon.ElementConverter;
import pl.droidsonroids.jspoon.annotation.Selector;

public interface BypassedElementConverter<T> extends ElementConverter<T> {
    @Override
    T convert(@Nullable Element node, @NonNull Selector selector);
}
