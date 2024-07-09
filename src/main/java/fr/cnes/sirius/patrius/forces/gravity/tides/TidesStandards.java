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
 * @history Created 17/07/2012
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:241:01/10/2014:improved tides conception
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

/**
 * Tides standards
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: TidesStandards.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 */
public class TidesStandards {

    /**
     * Tides standard enum
     */
    public enum TidesStandard {

        /**
         * IERS 1996 load factors
         */
        IERS1996,
        /**
         * IERS 2003 load factors
         */
        IERS2003,
        /**
         * GINS 2004 load factors
         */
        GINS2004;
    }
}
