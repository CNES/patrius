/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 *
 * @history 23/01/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:18/07/2013:Moved the Screw object and test class to commons math
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

/**
 * This class represents a screw
 * 
 * @version $Id: Screw.java 18108 2017-10-04 06:45:27Z bignon $
 * 
 * @since 3.1.2
 * 
 */
public class Screw {

    /** Origin. */
    private final Vector3D origin;
    /** Translation. */
    private final Vector3D translation;
    /** Origin. */
    private final Vector3D rotation;

    /**
     * Build a copy of a screw.
     * 
     * @param screw
     *        screw
     */
    public Screw(final Screw screw) {
        this.origin = screw.getOrigin();
        this.translation = screw.getTranslation();
        this.rotation = screw.getRotation();
    }

    /**
     * Constructor
     * 
     * @param inOrigin
     *        origin
     * @param inTranslation
     *        translation
     * @param inRotation
     *        rotation
     */
    public Screw(final Vector3D inOrigin, final Vector3D inTranslation, final Vector3D inRotation) {
        this.origin = inOrigin;
        this.translation = inTranslation;
        this.rotation = inRotation;
    }

    /**
     * Calculate the sum of this and screw, expressed in this objects origin.
     * 
     * @param s1
     *        screw.
     * @param s2
     *        screw.
     * @return sum of screws
     */
    public static Screw sum(final Screw s1, final Screw s2) {
        return new Screw(s1.origin, s1.translation.add(s2.translation),
            s1.rotation.add(displace(s2, s1.origin).rotation));
    }

    /**
     * Displace this screw, using Chasles
     * 
     * @param s
     *        screw
     * @param newOrigin
     *        new origin for s
     * @return screw expressed in new origin
     */
    public static Screw displace(final Screw s, final Vector3D newOrigin) {
        return new Screw(newOrigin, s.translation, s.rotation.add(s.origin.subtract(newOrigin).crossProduct(
            s.translation)));
    }

    /**
     * Calculate the sum of this and screw, expressed in this objects origin.
     * 
     * @param screw
     *        screw to add to this.
     * @return sum of screws
     */
    public Screw sum(final Screw screw) {
        return sum(this, screw);
    }

    /**
     * Displace this screw, using Chasles
     * 
     * @param newOrigin
     *        new origin for current object
     * @return screw expressed in new origin
     */
    public Screw displace(final Vector3D newOrigin) {
        return displace(this, newOrigin);
    }

    /**
     * @return the origin
     */
    public Vector3D getOrigin() {
        return this.origin;
    }

    /**
     * @return the translation
     */
    public Vector3D getTranslation() {
        return this.translation;
    }

    /**
     * @return the rotation
     */
    public Vector3D getRotation() {
        return this.rotation;
    }

    /**
     * Get a String representation of this screw.
     * 
     * @return a representation for this screw
     */
    @Override
    public String toString() {
        // Initialization
        final StringBuilder res = new StringBuilder();
        // Common variables
        final String fullClassName = this.getClass().getName();
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String open = "{";
        final String close = "}";
        final String comma = ",";
        // Add data
        res.append(shortClassName).append(open);
        res.append("Origin");
        res.append(this.origin.toString());
        res.append(comma);
        res.append("Translation");
        res.append(this.translation.toString());
        res.append(comma);
        res.append("Rotation");
        res.append(this.rotation.toString());
        res.append(close);
        // Return result
        //
        return res.toString();
    }

}
