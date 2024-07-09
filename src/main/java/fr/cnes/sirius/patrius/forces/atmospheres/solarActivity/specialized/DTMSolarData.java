/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.7:FA:FA-2853:18/05/2021:Erreurs documentation javadoc suite au refactoring modèle d'atmosphère DTM 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized;

import fr.cnes.sirius.patrius.forces.atmospheres.DTM2000;
import fr.cnes.sirius.patrius.forces.atmospheres.DTM2012;
import fr.cnes.sirius.patrius.forces.atmospheres.DTMInputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityToolbox;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a solar data container adapted for the {@link DTM2000} and {@link DTM2012} atmosphere models
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread-safe if SolarActivityDataProvider is thread-safe
 * 
 * @author Rami Houdroge
 * @since 1.2
 */
public class DTMSolarData implements DTMInputParameters {

    /** Serializable UID */
    private static final long serialVersionUID = -5212198874900835369L;
    /** Constant. */
    private static final int C_81 = 81;
    /** Constant */
    private static final double TO = 21;
    /** Constant */
    private static final int C_3600 = 3600;

    /**
     * Solar data
     */
    private final SolarActivityDataProvider data;

    /**
     * Constructor. Builds an instance of a solar data provider adapted for the {@link DTM2000} and {@link DTM2012}
     * atmosphere models
     * 
     * @param solarData
     *        input solar data
     */
    public DTMSolarData(final SolarActivityDataProvider solarData) {
        this.data = solarData;
    }

    /** {@inheritDoc} */
    @Override
    public double getInstantFlux(final AbsoluteDate date) throws PatriusException {
        return this.data.getInstantFluxValue(date);
    }

    /** {@inheritDoc} */
    @Override
    public double getMeanFlux(final AbsoluteDate date) throws PatriusException {
        return SolarActivityToolbox.getAverageFlux(date.shiftedBy(-C_81 * Constants.JULIAN_DAY / 2),
            date.shiftedBy(C_81 * Constants.JULIAN_DAY / 2), this.data);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMinDate() {
        return this.data.getMinDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMaxDate() {
        return this.data.getMaxDate();
    }

    /** {@inheritDoc} */
    @Override
    public double getThreeHourlyKP(final AbsoluteDate date) throws PatriusException {
        return this.data.getKp(date.shiftedBy(3 * C_3600));
    }

    /** {@inheritDoc} */
    @Override
    public double get24HoursKp(final AbsoluteDate date) throws PatriusException {
        return this.data.getKp(date.shiftedBy(TO * C_3600));
    }

    /** {@inheritDoc} */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        data.checkFluxValidity(start.shiftedBy(-C_81 * Constants.JULIAN_DAY / 2.),
                end.shiftedBy(C_81 * Constants.JULIAN_DAY / 2.));
        data.checkApKpValidity(start.shiftedBy(3 * C_3600), end.shiftedBy(TO * C_3600));
    }
}
