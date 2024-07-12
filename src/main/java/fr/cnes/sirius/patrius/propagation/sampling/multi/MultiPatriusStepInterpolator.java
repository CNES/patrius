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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:1872:10/10/2016:add Multi-attitude provider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.sampling.multi;

import java.util.Map;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * This interface is copied from {@link fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator} and adapted
 * to multi propagation.
 * </p>
 * <p>
 * This interface is a space-dynamics aware step interpolator.
 * </p>
 * <p>
 * It mirrors the <code>StepInterpolator</code> interface from <a href="http://commons.apache.org/math/">
 * commons-math</a> but provides a space-dynamics interface to the methods.
 * </p>
 * </p>
 * 
 * @author maggioranic
 * 
 * @version $Id: MultiOrekitStepInterpolator.java 18100 2017-10-03 10:04:21Z bignon $
 * 
 * @since 3.0
 * 
 */
public interface MultiPatriusStepInterpolator {

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
     * @see #getInterpolatedStates()
     */
    AbsoluteDate getInterpolatedDate();

    /**
     * Set the interpolated date.
     * <p>
     * It is possible to set the interpolation date outside of the current step range, but accuracy will decrease as
     * date is farther.
     * </p>
     * 
     * @param date
     *        interpolated date to set
     * @exception PropagationException
     *            if underlying interpolator cannot handle
     *            the date
     * @see #getInterpolatedDate()
     * @see #getInterpolatedStates()
     */
    void setInterpolatedDate(final AbsoluteDate date) throws PropagationException;

    /**
     * Get all the interpolated states.
     * 
     * @return interpolated states at the current interpolation date
     * @exception PatriusException
     *            if a state cannot be interpolated or converted
     * @see #getInterpolatedDate()
     * @see #setInterpolatedDate(AbsoluteDate)
     */
    Map<String, SpacecraftState> getInterpolatedStates() throws PatriusException;

    /**
     * Check is integration direction is forward in date.
     * 
     * @return true if integration is forward in date
     */
    boolean isForward();

}
