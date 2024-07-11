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
* VERSION:4.3:DM:DM-2082:15/05/2019:Modifications mineures d'api
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling;

import java.io.Serializable;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This interface is a space-dynamics aware step interpolator.
 * 
 * <p>
 * It mirrors the <code>StepInterpolator</code> interface from <a href="http://commons.apache.org/math/">
 * commons-math</a> but provides a space-dynamics interface to the methods.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public interface PatriusStepInterpolator extends Serializable {

    /**
     * Get the current grid date.
     * 
     * @return current grid date
     */
    AbsoluteDate getCurrentDate();

    /**
     * Get the previous grid date.
     * 
     * @return previous grid date
     */
    AbsoluteDate getPreviousDate();

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
    AbsoluteDate getInterpolatedDate();

    /**
     * Set the interpolated date.
     * <p>
     * It is possible to set the interpolation date outside of the current step range, but accuracy will decrease as
     * date is farther.
     * </p>
     * 
     * @param date interpolated date to set
     * @exception PropagationException if underlying interpolator cannot handle the date
     * @throws PatriusException if attitude cannot be computed
     * @see #getInterpolatedDate()
     * @see #getInterpolatedState()
     */
    void setInterpolatedDate(final AbsoluteDate date) throws PropagationException, PatriusException;

    /**
     * Get the interpolated state.
     * 
     * @return interpolated state at the current interpolation date
     * @exception PatriusException
     *            if state cannot be interpolated or converted
     * @see #getInterpolatedDate()
     * @see #setInterpolatedDate(AbsoluteDate)
     */
    SpacecraftState getInterpolatedState() throws PatriusException;

    /**
     * Check is integration direction is forward in date.
     * 
     * @return true if integration is forward in date
     */
    boolean isForward();

}
