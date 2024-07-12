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
 * @history Created 18/03/2014
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:180:18/03/2014:Grouped common methods and constants in an abstract class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized;

import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000InputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityToolbox;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This abstract class represents a solar data container adapted for the {@link MSISE2000} atmosphere model.
 * It implements the methods and constants common to the MSISE2000 data providers.
 * 
 * @see ContinuousMSISE2000SolarData
 * @see ClassicalMSISE2000SolarData
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread-safe if SolarActivityDataProvider and class implementation are thread-safe
 * 
 * @author Rami Houdroge
 * @version $Id: AbstractMSISE2000SolarData.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 2.2
 */
public abstract class AbstractMSISE2000SolarData implements MSISE2000InputParameters {

    /**
     * 81 days
     */
    protected static final int EO = 81;
    /**
     * Seconds in an hour
     */
    protected static final double HOUR = 3600;
    /**
     * Constant 12
     */
    protected static final double TWELVE = 12;
    /**
     * Constant 36
     */
    protected static final double THIRTY_SIX = 36;
    /**
     * Constant 60
     */
    protected static final double SIXTY = 60;
    /**
     * Constant 1.5
     */
    protected static final double ONE_POINT_FIVE = 1.5;
    /**
     * Constant 4.5
     */
    protected static final double FOUR_POINT_FIVE = 4.5;
    /**
     * Constant 7.5
     */
    protected static final double SEVEN_POINT_FIVE = 7.5;
    /**
     * Constant 10.5
     */
    protected static final double TEN_POINT_FIVE = 10.5;

     /** Serializable UID. */
    private static final long serialVersionUID = 7146293611912646073L;

    /**
     * Solar data container
     */
    protected SolarActivityDataProvider data;

    /**
     * Constructor. Builds an instance of a solar data provider adapted for the {@link MSISE2000} atmosphere model
     * 
     * @param solarData
     *        input solar data
     */
    public AbstractMSISE2000SolarData(final SolarActivityDataProvider solarData) {
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
        return SolarActivityToolbox.getAverageFlux(date.shiftedBy(-EO * Constants.JULIAN_DAY / 2),
            date.shiftedBy(EO * Constants.JULIAN_DAY / 2), this.data);
    }

    /** {@inheritDoc} */
    @Override
    public abstract double[] getApValues(final AbsoluteDate date) throws PatriusException;

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
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        data.checkFluxValidity(start.shiftedBy(-EO * Constants.JULIAN_DAY / 2.),
                end.shiftedBy(EO * Constants.JULIAN_DAY / 2.));
        data.checkApKpValidity(start.shiftedBy(-SIXTY * HOUR), end.shiftedBy(TWELVE * HOUR));
    }
}
