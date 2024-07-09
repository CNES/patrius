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
 * @history creation 03/04/2012
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (move IDirection in package attitudes.directions)
 * VERSION::DM:667:24/08/2016:add constructors
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.attitudes.directions.CelestialBodyPolesAxisDirection;
import fr.cnes.sirius.patrius.attitudes.directions.GenericTargetDirection;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.attitudes.directions.MomentumDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class implements a Sun pointing attitude law. The first direction is the satellite-sun direction, the second
 * direction is either the sun poles axis or the normal to the satellite orbit.
 * </p>
 * 
 * @concurrency unconditionally thread safe
 * 
 * @see IDirection
 * 
 * @author Julie Anton
 * 
 * @version $Id: SunPointing.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class SunPointing extends TwoDirectionsAttitude {

    /** IUD. */
    private static final long serialVersionUID = -580319565633029444L;

    /**
     * Constructor of the sun pointing attitude law. The first direction is the sun-satellite
     * direction, the second direction is the sun poles axis.
     * <p>
     * Sun is defined by {@link CelestialBodyFactory#getSun()} (JPL ephemeris model).
     * </p>
     * 
     * @param firstAxis : satellite axis aligned with the first direction.
     * @param secondAxis : satellite axis aligned at best with the second direction.
     * @throws PatriusException if the sun cannot be built
     */
    public SunPointing(final Vector3D firstAxis, final Vector3D secondAxis) throws PatriusException {
        this(firstAxis, secondAxis, CelestialBodyFactory.getSun());
    }

    /**
     * Constructor of the sun pointing attitude law. The first direction is the sun-satellite
     * direction, the second direction is the normal to the satellite orbit.
     * <p>
     * Sun is defined by {@link CelestialBodyFactory#getSun()} (JPL ephemeris model).
     * </p>
     * 
     * @param body : celestial body associated to the satellite orbit.
     * @param firstAxis : satellite axis aligned with the first direction.
     * @param secondAxis : satellite axis aligned at best with the second direction.
     * @throws PatriusException if the sun cannot be built
     */
    public SunPointing(final CelestialBody body, final Vector3D firstAxis, final Vector3D secondAxis)
        throws PatriusException {
        this(body, firstAxis, secondAxis, CelestialBodyFactory.getSun());
    }

    /**
     * Constructor of the sun pointing attitude law. The first direction is the sun-satellite
     * direction, the second direction is the sun poles axis.
     * 
     * @param firstAxis satellite axis aligned with the first direction.
     * @param secondAxis satellite axis aligned at best with the second direction.
     * @throws PatriusException if the sun cannot be built
     * @param sun sun
     */
    public SunPointing(final Vector3D firstAxis, final Vector3D secondAxis, final CelestialBody sun)
        throws PatriusException {
        super(new GenericTargetDirection(sun), new CelestialBodyPolesAxisDirection(sun), firstAxis,
            secondAxis);
    }

    /**
     * Constructor of the sun pointing attitude law. The first direction is the sun-satellite
     * direction, the second direction is the normal to the satellite orbit.
     * 
     * @param body celestial body associated to the satellite orbit.
     * @param firstAxis satellite axis aligned with the first direction.
     * @param secondAxis satellite axis aligned at best with the second direction.
     * @param sun sun
     */
    public SunPointing(final CelestialBody body, final Vector3D firstAxis,
        final Vector3D secondAxis, final CelestialBody sun) {
        super(new GenericTargetDirection(sun), new MomentumDirection(body), firstAxis, secondAxis);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s: firstAxis=%s, secondAxis=%s", this.getClass().getSimpleName(),
            this.getFirstAxis().toString(), this.getSecondAxis().toString());
    }
}
