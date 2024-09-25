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
 * @history created 18/03/2015
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * VERSION::DM:1872:10/12/2018:Substitution of AttitudeProvider with MultiAttitudeProvider
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.ode.ContinuousOutputModel;
import fr.cnes.sirius.patrius.math.ode.sampling.StepHandler;
import fr.cnes.sirius.patrius.math.ode.sampling.StepInterpolator;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.precomputed.multi.MultiIntegratedEphemeris;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * <p>
 * This interface is copied from {@link fr.cnes.sirius.patrius.propagation.numerical.EphemerisModeHandler} and adapted
 * to multi propagation.
 * </p>
 * <p>
 * This class stores sequentially generated orbital parameters of each states for later retrieval.
 * 
 * <p>
 * Instances of this class are built and then must be fed with the results provided by
 * {@link fr.cnes.sirius.patrius.propagation.MultiPropagator MultiPropagator} objects configured in
 * {@link fr.cnes.sirius.patrius.propagation.MultiPropagator#setEphemerisMode()
 * ephemeris generation mode}. Once propagation is over, a {@link BoundedPropagator} can be built for each spacecraft
 * from the stored steps.
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
public class MultiEphemerisModeHandler implements MultiModeHandler, StepHandler {

    /** Propagation orbit type. */
    private OrbitType orbitType;

    /** Position angle type. */
    private PositionAngle angleType;

    /** Map of attitude provider for forces computation. */
    private Map<String, MultiAttitudeProvider> attProvidersForces;

    /** Map of attitude provider for events computation. */
    private Map<String, MultiAttitudeProvider> attProvidersEvents;

    /** Informations about the global state vector. */
    private MultiStateVectorInfo stateInfo;

    /** Reference dates (for each propagation segment). */
    private List<AbsoluteDate> initializedReference;

    /** Map of frame for each spacecraft. */
    private Map<String, Frame> initializedFrame;

    /** Underlying raw mathematical model. */
    private List<ContinuousOutputModel> models;

    /** Map of generated ephemeris for each spacecraft. */
    private Map<String, BoundedPropagator> ephemeris;

    /** Flag for handler . */
    private boolean activate;

    /** Flag for forward/backward propagation. */
    private boolean forward;

    /** {@inheritDoc} */
    @Override
    public void initialize(final OrbitType orbit, final PositionAngle angle,
                           final Map<String, MultiAttitudeProvider> attitudeProvidersForces,
                           final Map<String, MultiAttitudeProvider> attitudeProvidersEvents,
                           final MultiStateVectorInfo stateVectorInfo, final boolean activateHandlers,
                           final AbsoluteDate reference, final Map<String, Frame> frame,
                           final Map<String, Double> mu) {
        // orbit
        this.orbitType = orbit;
        this.angleType = angle;
        this.initializedFrame = frame;
        // attitude providers
        this.attProvidersForces = attitudeProvidersForces;
        this.attProvidersEvents = attitudeProvidersEvents;
        // handlers
        this.activate = activateHandlers;
        this.initializedReference = new ArrayList<>();
        this.initializedReference.add(reference);
        this.models = new ArrayList<>();
        this.stateInfo = stateVectorInfo;
    }

    /** {@inheritDoc} */
    @Override
    public void setReference(final AbsoluteDate newReference) {
        if (forward) {
            // Forward propagation
            this.initializedReference.add(newReference);
        } else {
            // Retro-propagation
            this.initializedReference.add(0, newReference);
        }
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
     * Build ephemeris once and for all.
     */
    private void buildEphemeris() {
        try {
            // Set up the boundary dates
            final List<AbsoluteDate> startDates = new ArrayList<>();
            final List<AbsoluteDate> minDates = new ArrayList<>();
            final List<AbsoluteDate> maxDates = new ArrayList<>();

            // Loop on all models
            for (int i = 0; i < this.models.size(); i++) {
                final ContinuousOutputModel currentModel = this.models.get(i);
                final AbsoluteDate currentReference = this.initializedReference.get(i);

                final double tI = currentModel.getInitialTime();
                final double tF = currentModel.getFinalTime();
                final AbsoluteDate startDate = currentReference.shiftedBy(tI);
                // Min and Max dates for current model
                final AbsoluteDate minDate;
                final AbsoluteDate maxDate;
                if (tF < tI) {
                    // Retro-propagation
                    minDate = currentReference.shiftedBy(tF);
                    maxDate = startDate;
                } else {
                    // Propagation
                    minDate = startDate;
                    maxDate = currentReference.shiftedBy(tF);
                }
                startDates.add(startDate);
                minDates.add(minDate);
                maxDates.add(maxDate);
            }

            // Build ephemeris map

            // Get state vector informations
            final List<String> list = this.stateInfo.getIdList();
            final int sizeList = list.size();

            // Reset ephemeris
            this.ephemeris = new HashMap<>();
            // Loop on ID list
            for (int i = 0; i < sizeList; i++) {
                final String satId = list.get(i);
                final BoundedPropagator satEphemeris = new MultiIntegratedEphemeris(startDates,
                    minDates, maxDates, this.orbitType, this.angleType, this.attProvidersForces.get(satId),
                    this.attProvidersEvents.get(satId), this.stateInfo, this.models,
                    this.initializedFrame.get(satId), satId);
                this.ephemeris.put(satId, satEphemeris);
            }

        } catch (final PatriusException oe) {
            // Exception
            throw new PatriusExceptionWrapper(oe);
        }
    }

    /**
     * Get the generated ephemeris of the given spacecraft Id.
     * 
     * @param satId
     *        the spacecraft Id
     * @return a new instance of the generated ephemeris of the given spacecraft Id.
     * 
     */
    public BoundedPropagator getEphemeris(final String satId) {
        if (this.ephemeris == null) {
            this.buildEphemeris();
        }
        return this.ephemeris.get(satId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final double t0, final double[] y0, final double t) {
        if (t < t0) {
            // Retro-propagation
            this.models.add(0, new ContinuousOutputModel());
        } else {
            // Forward propagation
            this.models.add(new ContinuousOutputModel());
        }

        // Reinitialize ephemeris
        this.ephemeris = null;
    }

    /**
     * {@inheritDoc}
     */
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
}
