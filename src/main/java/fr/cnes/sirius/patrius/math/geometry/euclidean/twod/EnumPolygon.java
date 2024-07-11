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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.twod;

/**
 * Define the type of 2D polygon
 * 
 * @version $Id: EnumPolygon.java 18108 2017-10-04 06:45:27Z bignon $
 * @since 3.2
 */

public enum EnumPolygon {

    /** Degenerated Polygon : area is near zero */
    DEGENERATED("DEGENERATED"),

    /** Polygon with crossing border */
    CROSSING_BORDER("CROSSING_BORDER"),

    /** Well formed polygon (not degenerated, nor crossing), and convex */
    CONVEX("CONVEX"),

    /** Well formed polygon (not degenerated, nor crossing), and concave */
    CONCAVE("CONCAVE");

    /** Enumerate name */
    private final String name;

    /**
     * Enumerate name.
     * 
     * @param nameIn
     *        Enumerate name.
     */
    private EnumPolygon(final String nameIn) {
        this.name = nameIn;
    }

    /**
     * Getter for the enumerate name.
     * 
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * A well formed polygon is CONCAVE or CONVEX
     * 
     * @return boolean to indicate if polygon is well formed
     */
    public boolean isWellFormed() {
        return this == CONCAVE || this == CONVEX;

    }
}
