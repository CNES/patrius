/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass and attitude issues
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.precomputed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStampedCache;
import fr.cnes.sirius.patrius.time.TimeStampedGenerator;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

/**
 * This class is designed to accept and handle tabulated orbital entries.
 * Tabulated entries are classified and then extrapolated in way to obtain
 * continuous output, with accuracy and computation methods configured by the user.
 * 
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class Ephemeris extends AbstractPropagator implements BoundedPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -1523460404698149239L;

    /** First date in range. */
    private final AbsoluteDate minDate;

    /** Last date in range. */
    private final AbsoluteDate maxDate;

    /** Thread-safe cache. */
    private final transient TimeStampedCache<SpacecraftState> cache;

    /**
     * Constructor with tabulated states.
     * 
     * @param states
     *        tabulates states
     * @param interpolationPoints
     *        number of points to use in interpolation
     * @exception MathIllegalArgumentException
     *            if the number of states is smaller than
     *            the number of points to use in interpolation
     */
    public Ephemeris(final List<SpacecraftState> states, final int interpolationPoints) {

        super(null);

        if (states.size() < interpolationPoints) {
            throw new MathIllegalArgumentException(PatriusMessages.INSUFFICIENT_DIMENSION,
                states.size(), interpolationPoints);
        }

        this.minDate = states.get(0).getDate();
        this.maxDate = states.get(states.size() - 1).getDate();

        // set up cache
        final TimeStampedGenerator<SpacecraftState> generator =
            new TimeStampedGenerator<SpacecraftState>(){
                /** {@inheritDoc} */
                @Override
                public List<SpacecraftState> generate(final SpacecraftState existing, final AbsoluteDate date) {
                    return states;
                }
            };
        this.cache = new TimeStampedCache<SpacecraftState>(interpolationPoints,
            PatriusConfiguration.getCacheSlotsNumber(),
            Double.POSITIVE_INFINITY, Constants.JULIAN_DAY,
            generator, SpacecraftState.class);

    }

    /**
     * Get the first date of the range.
     * 
     * @return the first date of the range
     */
    @Override
    public AbsoluteDate getMinDate() {
        return this.minDate;
    }

    /**
     * Get the last date of the range.
     * 
     * @return the last date of the range
     */
    @Override
    public AbsoluteDate getMaxDate() {
        return this.maxDate;
    }

    @Override
    /** {@inheritDoc} */
    public SpacecraftState basicPropagate(final AbsoluteDate date) throws PropagationException {
        final SpacecraftState[] neighbors;
        try {
            neighbors = this.cache.getNeighbors(date);

        } catch (final TimeStampedCacheException tce) {
            throw new PropagationException(tce);
        }
        try {
            return neighbors[0].interpolate(date, Arrays.asList(neighbors));
        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }
    
    /**
     * Propagate an orbit without any fancy features.
     * <p>
     * This method is similar in spirit to the {@link #propagate} method, except that it does <strong>not</strong> call
     * any handler during propagation, nor any discrete events. It always stop exactly at the specified date.
     * </p>
     * 
     * @param date
     *        target date for propagation
     * @return orbit at specific date
     * @throws PropagationException
     *         if entries are not chronologically sorted or if new data cannot be generated
     *         if the sample points are inconsistent 
     */
    public Orbit basicPropagateOrbit(final AbsoluteDate date) throws PropagationException {
        final SpacecraftState[] neighbors;
        final List<Orbit> orbits;
        try {
            // get the neighbors 
            neighbors = this.cache.getNeighbors(date);
            // initialize orbits list
            orbits = new ArrayList<>(neighbors.length);
            // get a list of orbits from the neighbors 
            for (final SpacecraftState neighbor : neighbors){
                orbits.add(neighbor.getOrbit());
            }
            
        } catch (final TimeStampedCacheException tce) {
            throw new PropagationException(tce);
        }
        try {
            // return the propagated orbit
            return neighbors[0].interpolate(date, orbits);
        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        return this.basicPropagateOrbit(date);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.basicPropagateOrbit(date).getPVCoordinates(frame);
    }
    
    /** {@inheritDoc} */
    @Override
    public SpacecraftState getSpacecraftState(final AbsoluteDate date) throws PropagationException {
        return this.basicPropagate(date);
    }

    /**
     * Try (and fail) to reset the initial state.
     * <p>
     * This method always throws an exception, as ephemerides cannot be reset.
     * </p>
     * 
     * @param state
     *        new initial state to consider
     * @exception PropagationException
     *            always thrown as ephemerides cannot be reset
     */
    @Override
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        throw new PropagationException(PatriusMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState getInitialState() throws PropagationException {
        return this.basicPropagate(this.getMinDate());
    }

    /**
     * Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the data needed for generation, but does <em>not</em> serializes the
     * cache itself (in fact the cache is not serializable).
     * </p>
     * 
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        try {
            return new DataTransferObject(this.cache.getGenerator().generate(null, null),
                this.cache.getNeighborsSize());
        } catch (final TimeStampedCacheException tce) {
            // this should never happen
            throw PatriusException.createInternalError(tce);
        }
    }

    /** Internal class used only for serialization. */
    private static final class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -8479036196711159270L;

        /** Tabulates states. */
        private final List<SpacecraftState> states;

        /** Number of points to use in interpolation. */
        private final int interpolationPoints;

        /**
         * Simple constructor.
         * 
         * @param statesIn
         *        tabulates states
         * @param interpolationPointsIn
         *        number of points to use in interpolation
         */
        private DataTransferObject(final List<SpacecraftState> statesIn, final int interpolationPointsIn) {
            this.states = statesIn;
            this.interpolationPoints = interpolationPointsIn;
        }

        /**
         * Replace the deserialized data transfer object with a {@link Ephemeris}.
         * 
         * @return replacement {@link Ephemeris}
         */
        private Object readResolve() {
            // build a new provider, with an empty cache
            return new Ephemeris(this.states, this.interpolationPoints);
        }

    }
}
