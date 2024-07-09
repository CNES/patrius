/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history creation 8/02/2012
 * HISTORY
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:22/07/2013:Created the force application points properties
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:570:26/01/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:599:13/09/2016: new tabulated aero model
 * VERSION::DM:834:04/04/2017:create vehicle object
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

/**
 * This enumeration lists the possible types of properties that can be added to a part.
 * 
 * @see IPartProperty
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public enum PropertyType {
    /**
     * geometric property
     */
    GEOMETRY,
    /**
     * cross section property
     */
    CROSS_SECTION,
    /**
     * radiative sphere property (shape allowed for radiative model)
     */
    RADIATIVE_CROSS_SECTION,
    /**
     * radiative facet property (shape allowed for radiative model)
     */
    RADIATIVE_FACET,
    /**
     * radiative property (visible domain)
     */
    RADIATIVE,
    /**
     * radiative property (infrared domain)
     */
    RADIATIVEIR,
    /**
     * Aero facet property (shape allowed for aero model)
     */
    AERO_FACET,
    /**
     * Aero cross section property (shape allowed for aero model)
     */
    AERO_CROSS_SECTION,
    /**
     * mass property
     */
    MASS,
    /**
     * Inertia property
     */
    INERTIA,
    /**
     * sensor property
     */
    SENSOR,
    /**
     * RF property
     */
    RF,
    /**
     * drag property (example)
     */
    DRAG,
    /**
     * application point of solar radiation pressure
     */
    RADIATION_APPLICATION_POINT,
    /**
     * application point of drag force
     */
    AERO_APPLICATION_POINT,
    /**
     * global aerodynamic property (drag, lift and surface)
     */
    AERO_GLOBAL,
    /**
     * Wall property.
     */
    WALL,
    /**
     * Tank property.
     */
    TANK,
    /**
     * Propulsive property.
     */
    PROPULSIVE;
}
