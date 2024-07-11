/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history Created 20/08/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:131:28/10/2013:Renamed class
 * VERSION::FA:183:14/03/2014:Improved javadoc
 * VERSION::FA:180:18/03/2014:Grouped common methods and constants in an abstract class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized;

import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityToolbox;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a solar data container adapted for the {@link MSISE2000} atmosphere model
 * This model of input parameters computes averages for SOME of the ap values required by the MSISE2000 model.
 * See the {@link ClassicalMSISE2000SolarData#getApValues(AbsoluteDate)} and
 * {@link ContinuousMSISE2000SolarData#getApValues(AbsoluteDate)} methods.
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread-safe if SolarActivityDataProvider is thread-safe
 * 
 * @author Rami Houdroge
 * @version $Id: ClassicalMSISE2000SolarData.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 1.2
 */
public class ClassicalMSISE2000SolarData extends AbstractMSISE2000SolarData {

    /** Serial UID. */
    private static final long serialVersionUID = 7483647804864540620L;

    /** 7 */
    private static final int C_7 = 7;
    /** -3 */
    private static final int C_N3 = -3;
    /** -6 */
    private static final int C_N6 = -6;
    /** -9 */
    private static final int C_N9 = -9;

    /**
     * Constructor. Builds an instance of a solar data provider adapted for the {@link MSISE2000} atmosphere model
     * 
     * @param solarData
     *        input solar data
     */
    public ClassicalMSISE2000SolarData(final SolarActivityDataProvider solarData) {
        super(solarData);
    }

    /**
     * {@inheritDoc} <br>
     * 
     * <pre>
     * ap[0] = ap value averaged over [t0 - 12h; t0 + 12h]
     * ap[1] = ap value at given user date
     * ap[2] = ap value at given user date - 3 hours
     * ap[3] = ap value at given user date - 6 hours
     * ap[4] = ap value at given user date - 9 hours
     * ap[5] = ap value averaged over [t0 - 36h; t0 - 12h]
     * ap[6] = ap value averaged over [t0 - 61h; t0 - 36h]
     * 
     *  where t0 is the given user date (parameter "date")<br>
     * </pre>
     * 
     */
    @Override
    public double[] getApValues(final AbsoluteDate date) throws PatriusException {

        final double[] ap = new double[C_7];

        // daily AP
        ap[0] = SolarActivityToolbox.getMeanAp(date.shiftedBy(-TWELVE * HOUR),
            date.shiftedBy(TWELVE * HOUR), this.data);

        // 3 hr AP index for current time<br>
        ap[1] = this.data.getAp(date);
        ap[2] = this.data.getAp(date.shiftedBy(C_N3 * HOUR));
        ap[3] = this.data.getAp(date.shiftedBy(C_N6 * HOUR));
        ap[4] = this.data.getAp(date.shiftedBy(C_N9 * HOUR));
        ap[5] = SolarActivityToolbox.getMeanAp(date.shiftedBy(-THIRTY_SIX * HOUR),
            date.shiftedBy(-TWELVE * HOUR), this.data);
        ap[6] = SolarActivityToolbox.getMeanAp(date.shiftedBy(-SIXTY * HOUR),
            date.shiftedBy(-THIRTY_SIX * HOUR), this.data);

        return ap;
    }
}
