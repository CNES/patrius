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
 * @history creation 02/01/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.sp;

/**
 * Factory for predefined models.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: SPrimeModelFactory.java 18073 2017-10-02 16:48:07Z bignon $
 */
public final class SPrimeModelFactory {

    /** Ignore SP correction. */
    public static final SPrimeModel NO_SP = new NoSpCorrection();
    /** IERS 2003 convention. */
    public static final SPrimeModel SP_IERS2003 = new IERS2003SPCorrection();
    /** IERS 2010 convention. */
    public static final SPrimeModel SP_IERS2010 = new IERS2010SPCorrection();

    /** Private constructor. */
    private SPrimeModelFactory() {
    }
}
