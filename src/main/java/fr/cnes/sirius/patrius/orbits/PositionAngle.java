/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.orbits;

/**
 * Enumerate for true, eccentric and mean position angles.
 * 
 * @see KeplerianOrbit
 * @see CircularOrbit
 * @see EquinoctialOrbit
 * @see ApsisOrbit
 * @author Luc Maisonobe
 */
public enum PositionAngle {

    /** Mean angle. */
    MEAN,

    /** Eccentric angle. */
    ECCENTRIC,

    /** True angle. */
    TRUE;

}
