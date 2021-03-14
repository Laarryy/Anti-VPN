package me.egg82.antivpn.api.model.source.models;

import java.io.Serializable;

/**
 * An API source model which contains the raw response from a source. Cast this to the model you expect to receive.
 */
public interface SourceModel extends Serializable {
    /**
     * {@inheritDoc}
     */
    boolean equals(Object o);

    /**
     * {@inheritDoc}
     */
    int hashCode();

    /**
     * {@inheritDoc}
     */
    String toString();
}
