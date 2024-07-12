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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:377:08/12/2014:StepHandler initializing anomaly in propagator
 * VERSION::DM:389:10/03/2015:add interpolator copy method
 * VERSION::DM:300:18/03/2015:Creation multi propagator (Replaced AdditionalStateData by AdditionalStateInfo)
 * VERSION::FA:492:06/10/2015:Propagation until final date in master mode
 * VERSION::FA:838:09/03/2017:Corrected numerical quality issue on interpolated date
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling;

import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalStateInfo;
import fr.cnes.sirius.patrius.propagation.numerical.ModeHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Adapt an {@link fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler} to commons-math {@link StepHandler}
 * interface.
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class AdaptedStepHandler implements PatriusStepInterpolator, StepHandler, ModeHandler {

    /** Serializable UID. */
    private static final long serialVersionUID = -8067262257341902186L;

    /** Propagation orbit type. */
    private OrbitType orbitType;

    /** Position angle type. */
    private PositionAngle angleType;

    /** Attitude provider for forces computation. */
    private AttitudeProvider attProviderForces;

    /** Attitude provider for events computation. */
    private AttitudeProvider attProviderEvents;

    /** Additional states informations. */
    private Map<String, AdditionalStateInfo> addStateInfos;

    /** Reference date. */
    private AbsoluteDate initializedReference;

    /** Interpolated date ; by default, end of the current step. */
    private AbsoluteDate interpolatedDate;

    /** Reference frame. */
    private Frame initializedFrame;

    /** Central body attraction coefficient. */
    private double initializedMu;

    /** Underlying handler. */
    private final PatriusStepHandler handler;

    /** Flag for handler . */
    private boolean activate;

    /** Underlying raw rawInterpolator. */
    private StepInterpolator rawInterpolator;

    /**
     * Build an instance.
     * 
     * @param handlerIn
     *        underlying handler to wrap
     */
    public AdaptedStepHandler(final PatriusStepHandler handlerIn) {
        this.handler = handlerIn;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(final OrbitType orbit, final PositionAngle angle,
                           final AttitudeProvider attitudeProviderForces,
                           final AttitudeProvider attitudeProviderEvents,
                           final Map<String, AdditionalStateInfo> additionalStateInfos,
                           final boolean activateHandlers, final AbsoluteDate reference, final Frame frame,
                           final double mu) {
        this.orbitType = orbit;
        this.angleType = angle;
        this.attProviderForces = attitudeProviderForces;
        this.attProviderEvents = attitudeProviderEvents;
        this.addStateInfos = additionalStateInfos;
        this.activate = activateHandlers;
        this.initializedReference = reference;
        this.initializedFrame = frame;
        this.initializedMu = mu;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
        final AbsoluteDate date0 = this.initializedReference.shiftedBy(t0);
        final SpacecraftState s0 = new SpacecraftState(y0, this.orbitType, this.angleType, date0, this.initializedMu,
                this.initializedFrame, this.addStateInfos, this.attProviderForces, this.attProviderEvents);
        try {
            this.handler.init(s0, this.initializedReference.shiftedBy(t));
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /** {@inheritDoc} */
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
     * Get the current grid date.
     * 
     * @return current grid date
     */
    @Override
    public AbsoluteDate getCurrentDate() {
        return this.initializedReference.shiftedBy(this.rawInterpolator.getCurrentTime());
    }

    /**
     * Get the previous grid date.
     * 
     * @return previous grid date
     */
    @Override
    public AbsoluteDate getPreviousDate() {
        return this.initializedReference.shiftedBy(this.rawInterpolator.getPreviousTime());
    }

    /**
     * Get the interpolated date.
     * <p>
     * If {@link #setInterpolatedDate(AbsoluteDate) setInterpolatedDate} has not been called, the date returned is the
     * same as {@link #getCurrentDate() getCurrentDate}.
     * </p>
     * 
     * @return interpolated date
     * @see #setInterpolatedDate(AbsoluteDate)
     * @see #getInterpolatedState()
     */
    @Override
    public AbsoluteDate getInterpolatedDate() {
        return (this.interpolatedDate == null) ?
            this.initializedReference.shiftedBy(this.rawInterpolator.getInterpolatedTime()) : this.interpolatedDate;
    }

    /**
     * Set the interpolated date.
     * <p>
     * It is possible to set the interpolation date outside of the current step range, but accuracy will decrease as
     * date is farther.
     * </p>
     * 
     * @param date
     *        interpolated date to set
     * @see #getInterpolatedDate()
     * @see #getInterpolatedState()
     */
    @Override
    public void setInterpolatedDate(final AbsoluteDate date) {
        this.interpolatedDate = date;
        this.rawInterpolator.setInterpolatedTime(date.durationFrom(this.initializedReference));
    }

    /**
     * Get the interpolated state.
     * 
     * @return interpolated state at the current interpolation date
     * @exception PatriusException
     *            if state cannot be interpolated or converted
     * @see #getInterpolatedDate()
     * @see #setInterpolatedDate(AbsoluteDate)
     */
    @Override
    public SpacecraftState getInterpolatedState() throws PatriusException {
        final AbsoluteDate date = this.getInterpolatedDate();
        final double[] y = this.rawInterpolator.getInterpolatedState();
        return new SpacecraftState(y, this.orbitType, this.angleType, date, this.initializedMu, this.initializedFrame,
            this.addStateInfos,
            this.attProviderForces, this.attProviderEvents);
    }

    /**
     * Check is integration direction is forward in date.
     * 
     * @return true if integration is forward in date
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
     * <li>{@link #initializedMu} (primitive data type)</li>
     * <li>{@link #activate} (primitive data type)</li>
     * <li>{@link #rawInterpolator}</li>
     * </ul>
     * </p>
     * <p>
     * Following attributes reference is passed (no deep copy):
     * <ul>
     * <li>{@link #attProviderForces}</li>
     * <li>{@link #attProviderEvents}</li>
     * <li>{@link #addStateInfos}</li>
     * <li>{@link #initializedReference}</li>
     * <li>{@link #initializedFrame}</li>
     * <li>{@link #handler}</li>
     * </ul>
     * </p>
     * 
     * @return copy of this
     */
    public AdaptedStepHandler copy() {
        final AdaptedStepHandler res = new AdaptedStepHandler(this.handler);
        res.initialize(this.orbitType, this.angleType, this.attProviderForces, this.attProviderEvents,
            this.addStateInfos, this.activate,
            this.initializedReference, this.initializedFrame, this.initializedMu);
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
