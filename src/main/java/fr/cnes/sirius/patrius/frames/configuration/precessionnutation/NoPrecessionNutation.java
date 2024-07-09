/**
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
 * @history creation 06/02/2013
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * In this model the CIP doesnt move.
 * 
 * @author Rami Houdroge
 * 
 * @since 1.3
 * 
 * @version $Id: NoPrecessionNutation.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 */
public class NoPrecessionNutation implements PrecessionNutationModel {

    /** Serial UID. */
    private static final long serialVersionUID = -2495819055784347138L;

    /** {@inheritDoc} */
    @Override
    public double[] getCIPMotion(final AbsoluteDate t) {
        return new double[3];
    }

    /** {@inheritDoc} */
    @Override
    public double[] getCIPMotionTimeDerivative(final AbsoluteDate t) {
        return new double[3];
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirect() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return FrameConvention.NONE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConstant() {
        return true;
    }
}
