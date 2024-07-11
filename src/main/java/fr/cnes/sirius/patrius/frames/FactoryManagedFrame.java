/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:10/03/2016:serialize only necessary data
 * VERSION::DM:524:25/05/2016:serialization java doc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import java.io.ObjectStreamException;
import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Base class for the predefined frames that are managed by {@link FramesFactory}.
 * 
 * @serial an instance of FactoryManagedFrame is serializable,
 *         only the factory Key name is serialized as it allows to define
 *         entierly a FactoryManagedFrame.
 * @author Luc Maisonobe
 */
public class FactoryManagedFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 1566019035725009300L;

    /** Key of the frame within the factory. */
    private final Predefined factoryKey;

    /**
     * Simple constructor.
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transformProvider
     *        provider for transform from parent frame to instance
     * @param pseudoInertial
     *        true if frame is considered pseudo-inertial
     *        (i.e. suitable for propagating orbit)
     * @param factoryKeyIn
     *        key of the frame within the factory
     */
    public FactoryManagedFrame(final Frame parent, final TransformProvider transformProvider,
        final boolean pseudoInertial, final Predefined factoryKeyIn) {
        super(parent, transformProvider, factoryKeyIn.getName(), pseudoInertial);
        this.factoryKey = factoryKeyIn;
    }

    /**
     * Get the key of the frame within the factory.
     * 
     * @return key of the frame within the factory
     */
    public Predefined getFactoryKey() {
        return this.factoryKey;
    }

    /**
     * Replace the instance to be serialized with the instance of the proxy.
     * <p>
     * This proxy class serializes only the name of the key.
     * </p>
     * <p>
     * This conception pattern ensures that at most one instance of each {@link FactoryManagedFrame} exists even if it
     * is deserialized several times.
     * </p>
     * 
     * @return proxy object.
     */
    private Object writeReplace() {
        return new SerializationProxy(this.factoryKey);
    }

    /** Proxy class, only for serialization. */
    private static final class SerializationProxy implements Serializable {

        /**
         * serial Version UID.
         */
        private static final long serialVersionUID = -6426324005773630808L;

        /** Name of the frame. */
        private final String factoryKeyName;

        /**
         * constructor.
         * 
         * @param factoryKey
         *        key of the frame.
         */
        private SerializationProxy(final Predefined factoryKey) {
            this.factoryKeyName = factoryKey.name();
        }

        /**
         * Replace the deserialized data transfer object with a
         * FactoryManagedFrame.
         * 
         * @return a {@link FactoryManagedFrame}
         * @throws ObjectStreamException
         *         if object cannot be deserialized
         */
        private Object readResolve() throws ObjectStreamException {
            try {
                // retrieve a managed frame
                return FramesFactory.getFrame(Predefined.valueOf(this.factoryKeyName));
            } catch (final PatriusException oe) {
                throw new PatriusDeserializationException(oe.getLocalizedMessage(), oe);
            }
        }

    }

    /** Extended ObjectStreamException with some more information. */
    private static class PatriusDeserializationException extends ObjectStreamException {

        /** Serializable UID. */
        private static final long serialVersionUID = -4647126795776569854L;

        /**
         * Simple constructor. Build an exception from a cause and with a
         * specified message
         * 
         * @param message
         *        descriptive message
         * @param cause
         *        underlying cause
         */
        public PatriusDeserializationException(final String message, final Throwable cause) {
            super(message);
        }

    }

}
