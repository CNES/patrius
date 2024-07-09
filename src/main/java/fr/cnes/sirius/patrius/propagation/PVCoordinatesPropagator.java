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
 * @history created 25/09/2015
 * HISTORY
* VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:484:25/09/2015: Creation.
 * VERSION::DM:1173:24/08/2017:add propulsive and engine properties
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.util.List;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * This class is an analytical propagator which propagates states from the input PV, Attitude, and additional state
 * provider.
 * </p>
 * <p>
 * It can handle events and all functionalities from extended {@link AbstractPropagator} class.
 * </p>
 * 
 * <p>
 * The {@link AbstractPropagator#resetInitialState(SpacecraftState) resetInitialState action} will do nothing on this
 * propagator but is authorized to reset possible included attitude laws for instance.
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency.comment extends the AbstractPropagator class
 * 
 * @author chabaudp
 * 
 * @version $Id$
 * 
 * @since 3.1
 */
public class PVCoordinatesPropagator extends AbstractPropagator {

    /** Serializable UID */
    private static final long serialVersionUID = -7801485357930534815L;

    /** PVCoordinatesProvider used to propagate the position velocity */
    private final PVCoordinatesProvider pvProv;

    /**
     * Creates an instance of PVCoordinatePropagator without attitude and additional state providers
     * 
     * @param pvCoordProvider
     *        The position velocity coordinate provider used to propagate position velocity
     * @param initDate
     *        reference date
     * @param mu
     *        used for internal orbit parameter to convert orbital parameters type
     * @param frame
     *        used to express the pv coordinates
     * @throws PatriusException
     *         when a problem occurs in setting initial state
     * @throws IllegalArgumentException
     *         if frame is not pseudo inertial
     * 
     */
    public PVCoordinatesPropagator(final PVCoordinatesProvider pvCoordProvider,
        final AbsoluteDate initDate, final double mu, final Frame frame) throws PatriusException {
        this(pvCoordProvider, initDate, mu, frame, null, null, null);
    }

    /**
     * Creates an instance of PVCoordinatePropagator with
     * PV, attitude for forces, attitude for events, and additional state providers
     * given by the user.
     * 
     * @param pvCoordProvider
     *        The position velocity coordinate provider used to propagate position velocity
     * @param initDate
     *        reference date
     * @param mu
     *        used for internal orbit parameter to convert orbital parameters type
     * @param frame
     *        used to express the pv coordinates
     * @param attProviderForces
     *        The attitude provider used to compute forces. Can be null.
     * @param attProviderEvents
     *        The attitude provider used to compute events. Can be null.
     * @param additionalStateProviders
     *        The additional state providers used to propagate additional states. Can be null.
     * @throws PatriusException
     *         when a problem occurs in setting initial state
     * @throws IllegalArgumentException
     *         if frame is not pseudo inertial
     * 
     */
    public PVCoordinatesPropagator(final PVCoordinatesProvider pvCoordProvider, final AbsoluteDate initDate,
        final double mu, final Frame frame, final AttitudeProvider attProviderForces,
        final AttitudeProvider attProviderEvents,
        final List<AdditionalStateProvider> additionalStateProviders) throws PatriusException {

        super(attProviderForces, attProviderEvents);

        // Build the spacecraft initial state from the inputs date, pvCoordProvider, frame, and mu
        final CartesianOrbit orbit =
            new CartesianOrbit(pvCoordProvider.getPVCoordinates(initDate, frame), frame, initDate, mu);
        final SpacecraftState iniSpacecraftState = new SpacecraftState(orbit);
        //super.resetInitialState(iniSpacecraftState);
        this.resetInitialState(iniSpacecraftState);

        this.pvProv = pvCoordProvider;
        if (additionalStateProviders != null) {
            final int size = additionalStateProviders.size();
            for (int i = 0; i < size; i++) {
                this.addAdditionalStateProvider(additionalStateProviders.get(i));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        try {
            final Frame frame = this.getInitialState().getFrame();
            return new CartesianOrbit(this.pvProv.getPVCoordinates(date, frame), frame, date, this.getInitialState()
                .getMu());
        } catch (final PatriusException e) {
            throw new PropagationException(e);
        }
    }
}
