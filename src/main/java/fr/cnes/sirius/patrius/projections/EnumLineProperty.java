/**
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
package fr.cnes.sirius.patrius.projections;

/**
 * <p>
 * This enumeration allows user to know how polygon's points are connected. The enumeration can be of three kind :
 * </p>
 * <p>
 * <b> STRAIGHT </b> : A straight line is connecting each point. The associated projection is the
 * {@link IdentityProjection}. Use this if you're interested in polygonal computation speed.
 * </p>
 * <p>
 * <b> GREAT_CIRCLE </b> : A great circle line is the shortest way to connect two points on a sphere (or on ellipsoid).
 * Its associated projection method is a Gnomonic projection.
 * </p>
 * <p>
 * <b>STRAIGHT_RHUMB_LINE</b> : A straight rhumb line is a line of constant direction (constant bearing). The
 * {@link Mercator} projection basically satisfied this property and all rhumb lines are shown as straight lines.
 * </p>
 * <b>NONE</b> : No property.
 * </p>
 * 
 * @version $Id$
 * @since 3.2
 */
public enum EnumLineProperty {

    /**
     * Straight line property.
     */
    STRAIGHT("STRAIGHT"),

    /**
     * Great circle line property.
     */
    GREAT_CIRCLE("GREAT_CIRCLE"),

    /**
     * Rhumb line property.
     */
    STRAIGHT_RHUMB_LINE("STRAIGHT_RHUMB_LINE"),

    /**
     * No particular property
     */
    NONE("NONE");

    /** Line name. */
    private String name;

    /**
     * Constructor with a name.
     * 
     * @param nameIn
     *        of the type
     */
    private EnumLineProperty(final String nameIn) {
        this.name = nameIn;
    }

    /**
     * Get the line property's name.
     * 
     * @return name of the type
     */
    public String getName() {
        return this.name;
    }
}
