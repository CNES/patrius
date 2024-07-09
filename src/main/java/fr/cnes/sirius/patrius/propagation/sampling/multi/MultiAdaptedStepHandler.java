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
 * @history created 18/03/2015
 * 
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::FA:492:06/10/2015:Propagation until final date in master mode
 * VERSION::FA:1327:13/11/2017:change log message
 * VERSION::DM:1872:10/12/2018:Substitution of AttitudeProvider with MultiAttitudeProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling.multi;

import java.io.Serializable;
import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiModeHandler;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiStateVectorInfo;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * This class is copied from {@link fr.cnes.sirius.patrius.propagation.sampling.AdaptedStepHandler} and adapted to multi
 * propagation.
 * </p>
 * <p>
 * Adapt an {@link MultiPatriusStepHandler} to commons-math {@link StepHandler} interface.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public class MultiAdaptedStepHandler implements MultiPatriusStepInterpolator, StepHandler,
    MultiModeHandler, Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 7547717147491654384L;

    /** Propagation orbit type. */
    private OrbitType orbitType;

    /** Position angle type. */
    private PositionAngle angleType;

    /** Attitude provider for forces computation. */
    private Map<String, MultiAttitudeProvider> attitudeProvidersForces;

    /** Attitude provider for events computation. */
    private Map<String, MultiAttitudeProvider> attitudeProvidersEvents;

    /** Informations about the global state vector containing all spacecraft data. */
    private MultiStateVectorInfo stateInfo;

    /** Reference date. */
    private AbsoluteDate initializedReference;

    /** Interpolated date ; by default, end of the current step. */
    private AbsoluteDate interpolatedDate;

    /** Reference frame. */
    private Map<String, Frame> initializedFrames;

    /** Central body attraction coefficient. */
    private Map<String, Double> initializedMus;

    /** Underlying handler. */
    private final MultiPatriusStepHandler handler;

    /** Flag for handler . */
    private boolean activate;

    /** Underlying raw rawInterpolator. */
    private StepInterpolator rawInterpolator;

    /**
     * Build an instance.
     * 
     * @param multiHandler
     *        underlying handler to wrap
     */
    public MultiAdaptedStepHandler(final MultiPatriusStepHandler multiHandler) {
        this.handler = multiHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final OrbitType orbit, final PositionAngle angle,
                           final Map<String, MultiAttitudeProvider> attProvidersForces,
                           final Map<String, MultiAttitudeProvider> attProvidersEvents,
                           final MultiStateVectorInfo stateVectorInfo, final boolean activateHandlers,
                           final AbsoluteDate reference, final Map<String, Frame> frameMap,
                           final Map<String, Double> muMap) {
        this.orbitType = orbit;
        this.angleType = angle;
        this.attitudeProvidersForces = attProvidersForces;
        this.attitudeProvidersEvents = attProvidersEvents;
        this.stateInfo = stateVectorInfo;
        this.activate = activateHandlers;
        this.initializedReference = reference;
        this.initializedFrames = frameMap;
        this.initializedMus = muMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
        try {
            final AbsoluteDate date0 = this.initializedReference.shiftedBy(t0);
            final Map<String, SpacecraftState> s0 = this.stateInfo.mapArrayToStates(y0, date0,
                this.orbitType, this.angleType, this.attitudeProvidersForces, this.attitudeProvidersForces,
                this.initializedMus, this.initializedFrames);
            this.handler.init(s0, this.initializedReference.shiftedBy(t));
        } catch (final PatriusException oe) {
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleStep(final StepInterpolator interpolator, final boolean isLast) {
        try {
            this.rawInterpolator = interpolator.copy();
            this.interpolatedDate = null;
            if (this.activate) {
                this.handler.handleStep(this, isLast);
            }
        } catch (final PropagationException pe) {
            throw new PatriusExceptionWrapper(pe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbsoluteDate getCurrentDate() {
        return this.initializedReference.shiftedBy(this.rawInterpolator.getCurrentTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbsoluteDate getPreviousDate() {
        return this.initializedReference.shiftedBy(this.rawInterpolator.getPreviousTime());
    }

    /**
     * {@inheritDoc}
     * <p>
     * If {@link #setInterpolatedDate(AbsoluteDate) setInterpolatedDate} has not been called, the date returned is the
     * same as {@link #getCurrentDate() getCurrentDate}.
     * </p>
     */
    @Override
    public AbsoluteDate getInterpolatedDate() {
        return (this.interpolatedDate == null) ? this.initializedReference.shiftedBy(this.rawInterpolator
            .getInterpolatedTime()) : this.interpolatedDate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInterpolatedDate(final AbsoluteDate date) throws PropagationException {
        this.interpolatedDate = date;
        this.rawInterpolator.setInterpolatedTime(date.durationFrom(this.initializedReference));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, SpacecraftState> getInterpolatedStates() throws PatriusException {
        final AbsoluteDate date = this.getInterpolatedDate();
        final double[] y = this.rawInterpolator.getInterpolatedState();
        return this.stateInfo.mapArrayToStates(y, date, this.orbitType, this.angleType, this.attitudeProvidersForces,
            this.attitudeProvidersEvents, this.initializedMus, this.initializedFrames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isForward() {
        return this.rawInterpolator.isForward();
    }

    /**
     * <p>
     * Copy this.
     * </p>
     * <p>
     * Following attributes are deeply copied:
     * <ul>
     * <li>{@link #orbitType} (primitive data type)</li>
     * <li>{@link #angleType} (primitive data type)</li>
     * <li>{@link #activate} (primitive data type)</li>
     * <li>{@link #rawInterpolator}</li>
     * </ul>
     * </p>
     * <p>
     * Following attributes reference is passed (no deep copy):
     * <ul>
     * <li>{@link #attitudeProvidersForces}</li>
     * <li>{@link #attitudeProvidersEvents}</li>
     * <li>{@link #stateInfo}</li>
     * <li>{@link #initializedReference}</li>
     * <li>{@link #initializedMus}</li>
     * <li>{@link #initializedFrames}</li>
     * <li>{@link #handler}</li>
     * </ul>
     * </p>
     * 
     * @return copy of this
     */
    public MultiAdaptedStepHandler copy() {
        final MultiAdaptedStepHandler res = new MultiAdaptedStepHandler(this.handler);
        res.initialize(this.orbitType, this.angleType, this.attitudeProvidersForces, this.attitudeProvidersEvents,
            this.stateInfo, this.activate, this.initializedReference, this.initializedFrames, this.initializedMus);
        if (this.rawInterpolator != null) {
            res.rawInterpolator = this.rawInterpolator.copy();
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public void setReference(final AbsoluteDate newReference) {
        this.initializedReference = newReference;
    }
}
