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
 * @history Created 20/08/2012
 * 
 * HISTORY
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:131:28/10/2013:Created new MISISE2000 solar data class
 * VERSION::FA:183:14/03/2014:Improved javadoc
 * VERSION::FA:180:18/03/2014:Grouped common methods and constants in an abstract class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized;

import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityToolbox;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a solar data container adapted for the {@link MSISE2000} atmosphere model
 * This model of input parameters computes averages for ALL the ap values required by the MSISE2000 model.
 * See the {@link ClassicalMSISE2000SolarData#getApValues(AbsoluteDate)} and
 * {@link ContinuousMSISE2000SolarData#getApValues(AbsoluteDate)} methods.
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread-safe if SolarActivityDataProvider is thread-safe
 * 
 * @author Rami Houdroge
 * @version $Id: ContinuousMSISE2000SolarData.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 1.2
 */
public class ContinuousMSISE2000SolarData extends AbstractMSISE2000SolarData {

    /** Serial UID. */
    private static final long serialVersionUID = 6890443495729335881L;

    /** 7 */
    private static final int C_7 = 7;

    /**
     * Constructor. Builds an instance of a solar data provider adapted for the {@link MSISE2000} atmosphere model
     * 
     * @param solarData
     *        input solar data
     */
    public ContinuousMSISE2000SolarData(final SolarActivityDataProvider solarData) {
        super(solarData);
    }

    /** {@inheritDoc} This method returns the instant flux value at (date - 1 day). */
    @Override
    public double getInstantFlux(final AbsoluteDate date) throws PatriusException {
        return this.data.getInstantFluxValue(date.shiftedBy(-Constants.JULIAN_DAY));
    }

    /** {@inheritDoc} This method computes the trapezoidal average. */
    @Override
    public double getMeanFlux(final AbsoluteDate date) throws PatriusException {
        return SolarActivityToolbox.getMeanFlux(date.shiftedBy(-EO * Constants.JULIAN_DAY / 2),
            date.shiftedBy(EO * Constants.JULIAN_DAY / 2), this.data);
    }

    /**
     * {@inheritDoc} <br>
     * 
     * <pre>
     * ap[0] = ap value averaged over [t0 - 12h  ; t0 + 12h  ]
     * ap[1] = ap value averaged over [t0 -  1h30; t0 +  1h30]
     * ap[2] = ap value averaged over [t0 -  4h30; t0 -  1h30]
     * ap[3] = ap value averaged over [t0 -  7h30; t0 -  4h30]
     * ap[4] = ap value averaged over [t0 - 10h30; t0 -  7h30]
     * ap[5] = ap value averaged over [t0 - 36h  ; t0 - 12h  ]
     * ap[6] = ap value averaged over [t0 - 61h  ; t0 - 36h  ]
     * 
     *  where t0 is the given user date (parameter "date")<br>
     * </pre>
     * 
     */
    @Override
    public double[] getApValues(final AbsoluteDate date) throws PatriusException {

        final double[] ap = new double[C_7];

        ap[0] = SolarActivityToolbox.getMeanAp(date.shiftedBy(-TWELVE * HOUR),
            date.shiftedBy(TWELVE * HOUR), this.data);
        ap[1] = SolarActivityToolbox.getMeanAp(date.shiftedBy(-ONE_POINT_FIVE * HOUR),
            date.shiftedBy(ONE_POINT_FIVE * HOUR), this.data);
        ap[2] = SolarActivityToolbox.getMeanAp(date.shiftedBy(-FOUR_POINT_FIVE * HOUR),
            date.shiftedBy(-ONE_POINT_FIVE * HOUR), this.data);
        ap[3] = SolarActivityToolbox.getMeanAp(date.shiftedBy(-SEVEN_POINT_FIVE * HOUR),
            date.shiftedBy(-FOUR_POINT_FIVE * HOUR), this.data);
        ap[4] = SolarActivityToolbox.getMeanAp(date.shiftedBy(-TEN_POINT_FIVE * HOUR),
            date.shiftedBy(-SEVEN_POINT_FIVE * HOUR), this.data);
        ap[5] = SolarActivityToolbox.getMeanAp(date.shiftedBy(-THIRTY_SIX * HOUR),
            date.shiftedBy(-TWELVE * HOUR), this.data);
        ap[6] = SolarActivityToolbox.getMeanAp(date.shiftedBy(-SIXTY * HOUR),
            date.shiftedBy(-THIRTY_SIX * HOUR), this.data);

        return ap;
    }
}
