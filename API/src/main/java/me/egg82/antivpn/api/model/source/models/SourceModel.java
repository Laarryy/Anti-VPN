package me.egg82.antivpn.api.model.source.models;

import java.io.Serializable;

/**
 * An API source model which contains the raw response from a source. Cast this to the model you expect to receive.
 */
public interface SourceModel extends Serializable {
    /**
     * {@inheritDoc}
     */
    @Override
    boolean equals(Object o);

    /**
     * {@inheritDoc}
     */
    @Override
    int hashCode();

    /**
     * {@inheritDoc}
     */
    @Override
    String toString();
}
