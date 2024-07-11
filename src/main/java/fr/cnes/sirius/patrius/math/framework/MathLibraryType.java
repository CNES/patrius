/**
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
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.framework;

/**
 * Enumeration of Math library types currently available in PATRIUS.
 *
 * @author Emmanuel Bignon
 *
 * @since 4.2
 */
public enum MathLibraryType {

    /** Math from JDK. */
    MATH,

    /** StrictMath from JDK. */
    STRICTMATH,

    /** FastMath from Commons-Math. */
    FASTMATH,

    /** FastMath from JAFAMA which is a speed-up of FastMath with 1E-15 accuracy. */
    JAFAMA_FASTMATH,

    /** FastMath from JAFAMA which is a speed-up of FastMath with exact accuracy. */
    JAFAMA_STRICT_FASTMATH,

    /** Uses fastest library between FastMath, Jafama FastMath et Jafama StrictFastMath for each function. */
    FASTEST_MATHLIB;
}
