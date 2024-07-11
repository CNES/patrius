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
* VERSION:4.9:FA:FA-3150:10/05/2022:[PATRIUS] Absence d'attitude lors de l'utilisation du mode Ephemeris du propagateur 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:300:18/03/2015:Creation multi propagator (Replaced AdditionalStateData by AdditionalStateInfo)
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.ode.ContinuousOutputModel;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.precomputed.IntegratedEphemeris;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * This class stores sequentially generated orbital parameters for
 * later retrieval.
 * 
 * <p>
 * Instances of this class are built and then must be fed with the results provided by
 * {@link fr.cnes.sirius.patrius.propagation.Propagator Propagator} objects configured in
 * {@link fr.cnes.sirius.patrius.propagation.Propagator#setEphemerisMode()
 * ephemeris generation mode}. Once propagation is over, a {@link BoundedPropagator} can be built from the stored steps.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator
 * @author Luc Maisonobe
 */
class EphemerisModeHandler implements ModeHandler, StepHandler {

    /** Propagation orbit type. */
    private OrbitType orbitType;

    /** Position angle type. */
    private PositionAngle angleType;

    /** Attitude provider for forces computation. */
    private AttitudeProvider attitudeForcesProvider;

    /** Attitude provider for events computation. */
    private AttitudeProvider attitudeEventsProvider;

    /** Additional states informations. */
    private Map<String, AdditionalStateInfo> addStateInfos;

    /** Reference dates (for each propagation segment). */
    private List<AbsoluteDate> initializedReference;

    /** Frame. */
    private Frame initializedFrame;

    /** Central body gravitational constant. */
    private double initializedMu;

    /** Underlying raw mathematical model. */
    private List<ContinuousOutputModel> models;

    /** Flag for handler . */
    private boolean activate;

    /** Flag for forward/backward propagation. */
    private boolean forward;

    /** {@inheritDoc} */
    @Override
    public void initialize(final OrbitType orbit, final PositionAngle angle,
                           final AttitudeProvider attForcesProvider, final AttitudeProvider attEventsProvider,
                           final Map<String, AdditionalStateInfo> stateInfos, final boolean activateHandlers,
                           final AbsoluteDate reference, final Frame frame, final double mu) {
        // Attributes
        this.orbitType = orbit;
        this.angleType = angle;
        this.attitudeForcesProvider = attForcesProvider;
        this.attitudeEventsProvider = attEventsProvider;
        this.addStateInfos = stateInfos;
        this.activate = activateHandlers;
        // Initialize array
        this.initializedReference = new ArrayList<>();
        this.initializedReference.add(reference);
        this.initializedFrame = frame;
        this.initializedMu = mu;
        // Initialize array
        this.models = new ArrayList<>();
        // By default, propagation is forward
        this.forward = true;

    }

    /** {@inheritDoc} */
    @Override
    public void setReference(final AbsoluteDate newReference) {
        this.initializedReference.add(newReference);
    }

    /**
     * Set forward propagation flag.
     * 
     * @param isForward true if propagation is forward
     */
    public void setForward(final boolean isForward) {
        this.forward = isForward;
    }

    /**
     * Get the generated ephemeris.
     * 
     * @param attProvByDefault the attitude provider by default of the numerical propagator
     * @return a new instance of the generated ephemeris
     */
    public BoundedPropagator getEphemeris(final AttitudeProvider attProvByDefault) {
        try {
            // set up the boundary dates
            final List<AbsoluteDate> startDates = new ArrayList<>();
            final List<AbsoluteDate> minDates = new ArrayList<>();
            final List<AbsoluteDate> maxDates = new ArrayList<>();

            for (int i = 0; i < this.models.size(); i++) {
                // Loop on models
                final ContinuousOutputModel currentModel = this.models.get(i);
                final AbsoluteDate currentReference = this.initializedReference.get(i);

                final double tI = currentModel.getInitialTime();
                final double tF = currentModel.getFinalTime();
                final AbsoluteDate startDate = currentReference.shiftedBy(tI);
                final AbsoluteDate minDate;
                final AbsoluteDate maxDate;
                if (tF < tI) {
                    // Retro-propagation
                    minDate = currentReference.shiftedBy(tF);
                    maxDate = startDate;
                    startDates.add(0, startDate);
                    minDates.add(0, minDate);
                    maxDates.add(0, maxDate);
                } else {
                    // Propagation
                    minDate = startDate;
                    maxDate = currentReference.shiftedBy(tF);
                    startDates.add(startDate);
                    minDates.add(minDate);
                    maxDates.add(maxDate);
                }
            }

            // Define the integrated ephemeris
            final IntegratedEphemeris integrEphem;
            // Check if an attitude provider by default is present
            if (attProvByDefault == null) {
                // Use the attitude provider for forces computation and the attitude provider for event computation
                integrEphem = new IntegratedEphemeris(startDates, minDates, maxDates, this.orbitType, this.angleType,
                    this.attitudeForcesProvider, this.attitudeEventsProvider, this.addStateInfos, this.models,
                    this.initializedFrame, this.initializedMu);
            } else {
                // Use the attitude provider by default
                integrEphem = new IntegratedEphemeris(startDates, minDates, maxDates, this.orbitType, this.angleType,
                    attProvByDefault, this.addStateInfos, this.models, this.initializedFrame,
                    this.initializedMu);
            }
            // Return result
            return integrEphem;

        } catch (final PatriusException oe) {
            // Exception
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final StepInterpolator interpolator, final boolean isLast) {
        if (this.activate) {
            if (this.forward) {
                // Forward propagation
                this.models.get(this.models.size() - 1).handleStep(interpolator, isLast);
            } else {
                // Retro-propagation
                this.models.get(0).handleStep(interpolator, isLast);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
        if (t < t0) {
            // Retro-propagation
            this.models.add(0, new ContinuousOutputModel());
        } else {
            // Forward propagation
            this.models.add(new ContinuousOutputModel());
        }
    }
}
