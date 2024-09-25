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
 * @history Created 20/08/2012
 *
 * HISTORY
 * VERSION:4.13:FA:FA-165:08/12/2023:[PATRIUS] SolarActivityDataReader.getMaxDate erronee
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:FA:FA-1998:15/05/2019:[PATRIUS] Meilleur message d'erreur suite à un manque de données d'activité solaire
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:126:24/10/2013:Removed interpolation of ap values
 * VERSION::DM:131:28/10/2013:New solar data management system
 * VERSION::FA:180:18/03/2014:Added missing parameters in error messages
 * VERSION::FA:384:31/03/2015:Optimization of solar activity data provider
 * VERSION::FA:569:02/03/2016:Correction in case of UTC shift
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:846:20/02/2017: Deleted "static" on cache attributes
 * VERSION::FA:1134:16/11/2017: UTC management robustness
 * VERSION::FA:1448:20/04/2018:PATRIUS 4.0 minor corrections
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Represents a basic solar activity file reader. This class puts in common the same methods used by
 * solar activity file readers, and defines a common abstract class.
 * 
 * @concurrency not thread-safe
 * @concurrency.comment instance is mutable
 * 
 * @author Rami Houdroge
 * @version $Id: SolarActivityDataReader.java 17582 2017-05-10 12:58:16Z bignon $
 * @since 1.2
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class SolarActivityDataReader implements DataLoader, SolarActivityDataProvider {
    // CHECKSTYLE: resume AbstractClassName check

     /** Serializable UID. */
    private static final long serialVersionUID = 3995601801902272896L;

    /** Number of AP values */
    private static final int AP_VALUES_NB = 8;
    
    /**
     * Seconds in one day
     */
    private static final double SECONDS_IN_HOUR = 3600;
    
    /** Maximum offset */
    private static final int MAX_OFFSET = 7;
    
    /** Indicator for completed read. */
    protected boolean readCompleted;

    /** Map for flux values. */
    @SuppressWarnings("PMD.LooseCoupling")
    private final TreeMap<AbsoluteDate, Double> f107s = new TreeMap<>();

    /** Map for ap and kp values. */
    @SuppressWarnings("PMD.LooseCoupling")
    private final TreeMap<AbsoluteDate, Double[][]> aps = new TreeMap<>();

    /** Cached min date lower bound for flux values. */
    private AbsoluteDate cachedFluxsMind1 = AbsoluteDate.FUTURE_INFINITY;

    /** Cached min date upper bound for flux values. */
    private AbsoluteDate cachedFluxsMaxd1 = AbsoluteDate.FUTURE_INFINITY;

    /** Cached max date lower bound for flux values. */
    private AbsoluteDate cachedFluxsMind2 = AbsoluteDate.FUTURE_INFINITY;

    /** Cached max date upper bound for flux values. */
    private AbsoluteDate cachedFluxsMaxd2 = AbsoluteDate.FUTURE_INFINITY;

    /** Cached date lower bound for flux value. */
    private AbsoluteDate cachedFluxMind = AbsoluteDate.FUTURE_INFINITY;

    /** Cached date upper bound for flux value. */
    private AbsoluteDate cachedFluxMaxd = AbsoluteDate.FUTURE_INFINITY;

    /** Cached date lower bound for Ap/Kp value. */
    private AbsoluteDate cachedApKpMind = AbsoluteDate.FUTURE_INFINITY;

    /** Cached date upper bound for Ap/Kp value. */
    private AbsoluteDate cachedApKpMaxd = AbsoluteDate.FUTURE_INFINITY;

    /**
     * Cached flux valid between [[cachedFluxsMind1, cachedFluxsMaxd1], [cachedFluxsMind2,
     * cachedFluxsMaxd2]].
     */
    private SortedMap<AbsoluteDate, Double> cachedFluxs = null;

    /** Cached flux valid between [cachedFluxMind, cachedFluxMaxd]. */
    private double cachedFlux1 = Double.POSITIVE_INFINITY;

    /** Cached flux valid between [cachedFluxMind, cachedFluxMaxd]. */
    private double cachedFlux2 = Double.POSITIVE_INFINITY;

    /** Cached difference between cachedFluxMind and cachedFluxMaxd. */
    private double cachedFluxDt = 0;

    /** Cached Ap/Kp valid between [cachedApMind, cachedApMaxd[. */
    private Double[][] cachedApKp = null;

    /** Supported names. */
    private final String names;

    /**
     * Constructor.
     * <p>
     * Build an uninitialized reader.
     * </p>
     * 
     * @param supportedNames regular expression for supported files names
     */
    protected SolarActivityDataReader(final String supportedNames) {
        this.names = supportedNames;
    }

    /**
     * {@inheritDoc}
     * Warning: for performance reasons, this method does not check if data are available for provided date.
     * If not data is available for provided date, null is returned.
     * The method {@link #checkApKpValidity(AbsoluteDate, AbsoluteDate)} should be called to ensure data are available
     * for provided date.
     */
    @Override
    public SortedMap<AbsoluteDate, Double[]> getApKpValues(final AbsoluteDate date1,
                                                           final AbsoluteDate date2) {
        // create a map with a date as a key and a two row table
        final TreeMap<AbsoluteDate, Double[]> result = new TreeMap<>();

        // put first value
        result.put(date1, new Double[] { this.getAp(date1), this.getKp(date1) });

        // put last value
        result.put(date2, new Double[] { this.getAp(date2), this.getKp(date2) });

        // build up an array of AbsoluteDate for which there is an ap / kp value change
        final AbsoluteDate startScan = this.aps.floorKey(date1);

        // each of the dates above has 8 ap values that we need to account for
        AbsoluteDate current = startScan;
        while (current.compareTo(date2) < 0) {
            for (int i = 0; i < AP_VALUES_NB; i++) {
                current = current.shiftedBy(3 * SECONDS_IN_HOUR);
                if (current.compareTo(date1) > 0 && current.compareTo(date2) < 0) {
                    result.put(current, new Double[] { this.getAp(current), this.getKp(current) });
                }
            }
            current = this.aps.ceilingKey(current);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * Warning: for performance reasons, this method does not check if data are available for provided date.
     * If not data is available for provided date, null is returned.
     * The method {@link #checkApKpValidity(AbsoluteDate, AbsoluteDate)} should be called to ensure data are available
     * for provided date.
     */
    @Override
    public SortedMap<AbsoluteDate, Double> getInstantFluxValues(final AbsoluteDate date1,
                                                                final AbsoluteDate date2) {

        // Check if cached data can be used
        final boolean d1Valid = (date1.durationFrom(this.cachedFluxsMind1) >= 0 && date1
            .durationFrom(this.cachedFluxsMaxd1) <= 0);
        final boolean d2Valid = (date2.durationFrom(this.cachedFluxsMind2) >= 0 && date2
            .durationFrom(this.cachedFluxsMaxd2) <= 0);

        if (!d1Valid || !d2Valid) {
            // Invalidate cache
            this.cachedFluxsMind1 = this.f107s.floorKey(date1);
            this.cachedFluxsMaxd1 = this.f107s.ceilingKey(date1);
            this.cachedFluxsMind2 = this.f107s.floorKey(date2);
            this.cachedFluxsMaxd2 = this.f107s.ceilingKey(date2);
            this.cachedFluxs = this.f107s.subMap(date1, true, date2, true);
        }
        return this.cachedFluxs;
    }

    /**
     * {@inheritDoc} This is the default implementation for this method : it interpolates the flux
     * values before or after
     */
    @Override
    public double getInstantFluxValue(final AbsoluteDate date) {

        if (this.f107s.containsKey(date)) {
            return this.f107s.get(date);
        }

        // Check if cached data can be used
        final boolean dValid = (date.durationFrom(this.cachedFluxMind) >= 0 && date
            .durationFrom(this.cachedFluxMaxd) <= 0);

        if (!dValid) {
            // Invalidate cache
            final Entry<AbsoluteDate, Double> floor = this.f107s.floorEntry(date);
            final Entry<AbsoluteDate, Double> ceiling = this.f107s.ceilingEntry(date);

            // invalid floor or ceiling
            if (floor == null || ceiling == null) {
                throw new PatriusExceptionWrapper(new PatriusException(
                    PatriusMessages.NO_SOLAR_ACTIVITY_AT_DATE, date, this.f107s.firstKey(),
                    this.f107s.lastKey()));
            }

            this.cachedFluxMind = floor.getKey();
            this.cachedFluxMaxd = ceiling.getKey();
            this.cachedFlux1 = floor.getValue();
            this.cachedFlux2 = ceiling.getValue();
            this.cachedFluxDt = this.cachedFluxMaxd
                .offsetFrom(this.cachedFluxMind, TimeScalesFactory.getTAI());
        }

        // Compute value
        final double dt = date.offsetFrom(this.cachedFluxMind, TimeScalesFactory.getTAI());
        return this.cachedFlux1 + MathLib.divide(dt, this.cachedFluxDt) * (this.cachedFlux2 - this.cachedFlux1);
    }

    /**
     * {@inheritDoc}
     * Warning: for performance reasons, this method does not check if data are available for provided date.
     * If not data is available for provided date, null is returned.
     * The method {@link #checkApKpValidity(AbsoluteDate, AbsoluteDate)} should be called to ensure data are available
     * for provided date.
     */
    @Override
    public double getAp(final AbsoluteDate date) {

        // Check if cached data can be used
        final boolean dValid = (date.durationFrom(this.cachedApKpMind) >= 0 && date
            .durationFrom(this.cachedApKpMaxd) < 0);

        if (!dValid) {
            // Invalidate cache
            final Entry<AbsoluteDate, Double[][]> floor = this.aps.floorEntry(date);
            this.cachedApKpMind = floor.getKey();
            this.cachedApKp = floor.getValue();
            this.cachedApKpMaxd = this.aps.ceilingKey(date);
        }

        int offset = (int) MathLib.floor((date.offsetFrom(this.cachedApKpMind,
            TimeScalesFactory.getTAI()) / SECONDS_IN_HOUR) / 3.);
        offset = MathLib.min(offset, MAX_OFFSET);
        return this.cachedApKp[0][offset];
    }

    /**
     * {@inheritDoc}
     * Warning: for performance reasons, this method does not check if data are available for provided date.
     * If not data is available for provided date, null is returned.
     * The method {@link #checkApKpValidity(AbsoluteDate, AbsoluteDate)} should be called to ensure data are available
     * for provided date.
     */
    @Override
    public double getKp(final AbsoluteDate date) {

        // Check if cached data can be used
        final boolean dValid = (date.durationFrom(this.cachedApKpMind) >= 0 && date
            .durationFrom(this.cachedApKpMaxd) < 0);

        if (!dValid) {
            // Invalidate cache
            final Entry<AbsoluteDate, Double[][]> floor = this.aps.floorEntry(date);
            this.cachedApKpMind = floor.getKey();
            this.cachedApKp = floor.getValue();
            this.cachedApKpMaxd = this.aps.ceilingKey(date);
        }

        int offset = (int) MathLib.floor((date.offsetFrom(this.cachedApKpMind,
            TimeScalesFactory.getTAI()) / SECONDS_IN_HOUR) / 3.);
        offset = MathLib.min(offset, MAX_OFFSET);
        return this.cachedApKp[1][offset];
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMinDate() {
        if (this.getFluxMinDate().compareTo(this.getApKpMinDate()) <= 0) {
            return this.getApKpMinDate();
        }
        return this.getFluxMinDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getMaxDate() {
        if (this.getFluxMaxDate().compareTo(this.getApKpMaxDate()) <= 0) {
            return this.getFluxMaxDate();
        }
        return this.getApKpMaxDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getFluxMinDate() {
        return this.f107s.firstKey();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getFluxMaxDate() {
        return this.f107s.lastKey();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getApKpMinDate() {
        return this.aps.firstKey();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getApKpMaxDate() {
        return this.aps.lastKey();
    }

    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return !this.readCompleted;
    }

    /** {@inheritDoc} */
    @Override
    public abstract void loadData(InputStream input, String name) throws IOException,
                                                                 ParseException, PatriusException;

    /**
     * Get the regular expression for supported files names.
     * 
     * @return regular expression for supported files names
     */
    public String getSupportedNames() {
        return this.names;
    }

    /**
     * Add a flux value
     * 
     * @param date date of flux value
     * @param f107 value of flux at date
     */
    protected void addF107(final AbsoluteDate date, final double f107) {
        synchronized (this.f107s) {
            this.f107s.put(date, f107);
        }
    }

    /**
     * Add a flux value
     * 
     * @param date date of flux value
     * @param apkp coefficients at date : double[2][8]
     */
    protected void addApKp(final AbsoluteDate date, final Double[][] apkp) {
        synchronized (this.aps) {
            this.aps.put(date, apkp);
        }
    }

    /**
     * Chech if data map is empty
     * 
     * @return true if it is
     */
    protected boolean isEmpty() {
        synchronized (this.f107s) {
            synchronized (this.aps) {
                return this.f107s.isEmpty() && this.aps.isEmpty();
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public double getStepApKp() throws PatriusException {
        return getApKpMaxDate().durationFrom(getApKpMinDate(), TimeScalesFactory.getUTC())
                / (getApKpValues(getApKpMinDate(), getApKpMaxDate()).size() - 1);
    }
    
    /** {@inheritDoc} */
    @Override
    public double getStepF107() throws PatriusException {
        return getFluxMaxDate().durationFrom(getFluxMinDate(), TimeScalesFactory.getUTC())
                / (getInstantFluxValues(getFluxMinDate(), getFluxMaxDate()).size() - 1);
    }
}
