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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling;

import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Implementation of the {@link PatriusStepInterpolator} interface based
 * on a {@link Propagator}.
 * 
 * @author Luc Maisonobe
 */
public class BasicStepInterpolator implements PatriusStepInterpolator {

    /** Serializable UID. */
    private static final long serialVersionUID = 7847540541046397037L;

    /** Underlying propagator. */
    private final Propagator propagator;

    /** Previous date. */
    private AbsoluteDate previousDate;

    /** Current date. */
    private AbsoluteDate currentDate;

    /** Interpolated State. */
    private SpacecraftState interpolatedState;

    /** Forward propagation indicator. */
    private boolean forward;

    /**
     * Build a new instance from a basic propagator.
     * 
     * @param propagatorIn
     *        underlying propagator to use
     */
    public BasicStepInterpolator(final Propagator propagatorIn) {
        this.propagator = propagatorIn;
        this.previousDate = AbsoluteDate.PAST_INFINITY;
        this.currentDate = AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getCurrentDate() {
        return this.currentDate;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getInterpolatedDate() {
        return this.interpolatedState.getDate();
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState getInterpolatedState() throws PatriusException {
        return this.interpolatedState;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getPreviousDate() {
        return this.previousDate;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isForward() {
        return this.forward;
    }

    /** {@inheritDoc} */
    @Override
    public void setInterpolatedDate(final AbsoluteDate date) throws PropagationException {
        this.interpolatedState = this.propagator.propagate(date);
    }

    /**
     * Shift one step forward.
     * Copy the current date into the previous date, hence preparing the
     * interpolator for future calls to {@link #storeDate storeDate}
     */
    public void shift() {
        this.previousDate = this.currentDate;
    }

    /**
     * Store the current step date.
     * 
     * @param date
     *        current date
     * @exception PropagationException
     *            if the state cannot be propagated at specified date
     */
    public void storeDate(final AbsoluteDate date) throws PropagationException {
        this.currentDate = date;
        this.forward = this.currentDate.compareTo(this.previousDate) >= 0;
        this.setInterpolatedDate(this.currentDate);
    }

}
