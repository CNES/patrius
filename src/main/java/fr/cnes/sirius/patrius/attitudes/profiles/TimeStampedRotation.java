/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * {@link TimeStamped Time-stamped} version of {@link Rotation}.
 *
 * @author Pierre Brechard
 *
 * @since 4.4
 */
public final class TimeStampedRotation extends Rotation implements TimeStamped {

    /** Serialization UID. */
    private static final long serialVersionUID = -4111292762954435584L;
    
    /** Factor to compute hash code */
    private static final int HASHCODEFACTOR = 37;

    /** The date. */
    private final AbsoluteDate date;

    /**
     * Constructor.
     * @param rotation the underlying rotation
     * @param date the date
     */
    public TimeStampedRotation(final Rotation rotation, final AbsoluteDate date) {
        super(false, rotation.getQuaternion());
        this.date = date;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        final boolean equal;
        if (this == object) {
            equal = true;
        } else if (object instanceof TimeStampedRotation) {
            final TimeStampedRotation rotation = (TimeStampedRotation) object;
            equal = getQuaternion().equals(rotation.getQuaternion()) && this.date.equals(rotation.getDate());

        } else {
            equal = false;
        }
        return equal;
    }

    /**
     * Get the hash code for the time-stamped rotation object.
     * <p>
     * Based on <em>Josh Bloch</em>'s <em><strong>Effective Java</strong></em>, Item 8
     * @return the hash code
     */
    @Override
    public int hashCode() {
        // Use a combination of a prime number and the unique hash codes of the underlying date and rotation objects
        return (HASHCODEFACTOR * this.date.hashCode()) + getQuaternion().hashCode();
    }
}
