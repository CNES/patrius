/**
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
 * @history creation 11/10/2012
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;

/**
 * IERS Precession Nutation enumerate. Each enumerate provides data file locations.
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: PrecessionNutationConvention.java 18073 2017-10-02 16:48:07Z bignon $
 */
public enum PrecessionNutationConvention {

    /** IERS 2003 Precession Nutation convention. */
    IERS2003(FrameConvention.IERS2003) {
        /** {@inheritDoc} */
        @Override
        protected String[] getDataLocation() {
            return new String[] { S03 + TA, S03 + TB, S03 + TC };
        }
    },
    /** IERS 2010 Precession Nutation convention. */
    IERS2010(FrameConvention.IERS2010) {
        /** {@inheritDoc} */
        @Override
        protected String[] getDataLocation() {
            return new String[] { S10 + TA, S10 + TB, S10 + TD };
        }
    };

    /** File locations. */
    private static final String S03 = "/META-INF/IERS-conventions-2003";
    /** File locations. */
    private static final String S10 = "/META-INF/IERS-conventions-2010";
    /** File names. */
    private static final String TA = "/tab5.2a.txt";
    /** File names. */
    private static final String TB = "/tab5.2b.txt";
    /** File names. */
    private static final String TC = "/tab5.2c.txt";
    /** File names. */
    private static final String TD = "/tab5.2d.txt";

    /** IERS convention. */
    private final FrameConvention iersConvention;

    /**
     * COnstructor.
     * 
     * @param iersConv
     *        IERS convention
     */
    private PrecessionNutationConvention(final FrameConvention iersConv) {
        this.iersConvention = iersConv;
    }

    /**
     * Get the location of the data for the selected convention.
     * 
     * @return location
     */
    protected abstract String[] getDataLocation();

    /**
     * Getter for IERS convention.
     * 
     * @return IERS convention
     */
    public FrameConvention getIERSConvention() {
        return this.iersConvention;
    }
}
