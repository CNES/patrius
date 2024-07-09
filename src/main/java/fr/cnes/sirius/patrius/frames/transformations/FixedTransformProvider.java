/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.7:DM:DM-2818:18/05/2021:[PATRIUS|COLOSUS] Classe GatesModel
* VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:524:25/05/2016:serialization java doc
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 ** Transform provider using fixed transform.
 *
 * <p>
 * Spin derivative available only if defined in the transformation at construction.
 * </p>
 * <p>
 * Frames configuration is unused.
 * </p>
 * 
 * @serial serializable.
 * @author Luc Maisonobe
 */
public class FixedTransformProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 7143912747227560905L;

    /** Fixed transform. */
    private final Transform transform;

    /**
     * Simple constructor.
     * 
     * @param transformIn
     *        fixed transform
     */
    public FixedTransformProvider(final Transform transformIn) {
        this.transform = transformIn;
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        return this.transform;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config) {
        return this.transform;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivative available only if defined in the transformation at construction.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.transform;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Spin derivative available only if defined in the transformation at construction.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    @Override
    public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                  final boolean computeSpinDerivatives) throws PatriusException {
        return this.transform;
    }

}
