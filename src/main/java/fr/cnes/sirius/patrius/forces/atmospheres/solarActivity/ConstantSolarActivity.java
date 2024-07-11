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
 * @history Created 25/04/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:131:25/10/2013:New solar activity architecture
 * VERSION::FA:180:18/03/2014:Added FA 71 history
 * VERSION::FA:345:03/11/2014: coverage that leads to finding a bug in the second constructor in the if loop
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.util.SortedMap;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents constant solar activity
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * @version $Id: ConstantSolarActivity.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 1.1
 * 
 */
public class ConstantSolarActivity implements SolarActivityDataProvider {

    /** Serial UID. */
    private static final long serialVersionUID = -3992556726807633640L;

    /**
     * instant flux
     */
    private final double instant;
    /**
     * geomag value
     */
    private final double aap;
    /**
     * geomag value
     */
    private final double kkp;
    /**
     * data
     */
    private final Double[] table;

    /**
     * Constructor for constant solar activity
     * 
     * @param f107
     *        instant flux
     * @param ap
     *        ap value
     */
    public ConstantSolarActivity(final double f107, final double ap) {
        this(f107, ap, SolarActivityToolbox.apToKp(ap));
    }

    /**
     * Constructor for constant solar activity
     * 
     * @param f107
     *        instant flux
     * @param ap
     *        ap value
     * @param kp
     *        kp value
     */
    public ConstantSolarActivity(final double f107, final double ap, final double kp) {

        SolarActivityToolbox.checkApSanity(ap);
        if (kp != SolarActivityToolbox.apToKp(ap)) {
            throw new IllegalArgumentException();
        }

        // store data
        this.instant = f107;
        this.aap = ap;
        this.kkp = kp;
        this.table = new Double[] { this.aap, this.kkp };
    }

    /**
     * Get the value of the instantaneous solar flux.
     * 
     * @param date
     *        the current date
     * @return the instantaneous solar flux
     */
    public double getInstantFlux(final AbsoluteDate date) {
        return this.instant;
    }

    /** {@inheritDoc} */
    @Override
    public double getAp(final AbsoluteDate date) {
        return this.aap;
    }

    /** {@inheritDoc} */
    @Override
    public double getKp(final AbsoluteDate date) {
        return this.kkp;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getFluxMinDate() {
        return AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getFluxMaxDate() {
        return AbsoluteDate.FUTURE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getApKpMinDate() {
        return AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getApKpMaxDate() {
        return AbsoluteDate.FUTURE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public SortedMap<AbsoluteDate, Double> getInstantFluxValues(final AbsoluteDate date1, final AbsoluteDate date2) {
        final TreeMap<AbsoluteDate, Double> result = new TreeMap<AbsoluteDate, Double>();
        result.put(date1, this.instant);
        result.put(date2, this.instant);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public double getInstantFluxValue(final AbsoluteDate date) throws PatriusException {
        return this.instant;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMinDate() {
        return AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMaxDate() {
        return AbsoluteDate.FUTURE_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public SortedMap<AbsoluteDate, Double[]> getApKpValues(final AbsoluteDate date1, final AbsoluteDate date2) {
        final TreeMap<AbsoluteDate, Double[]> result = new TreeMap<AbsoluteDate, Double[]>();
        result.put(date1, this.table);
        result.put(date2, this.table);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public double getStepApKp() throws PatriusException {
        // Step by default to one julian day
        return Constants.JULIAN_DAY;
    }

    /** {@inheritDoc} */
    @Override
    public double getStepF107() throws PatriusException {
        // Step by default to one julian day
        return Constants.JULIAN_DAY;
    }
}
