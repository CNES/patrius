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
 * @history 30/08/2016 Creation of the class
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:586:30/08/2016:ISIS Sun pointing law
 * VERSION::FA:1451:09/03/2018: Normalization of xSun
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.attitudes.directions.GenericTargetDirection;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of ISIS Sun pointing attitude law.
 * This class implements {@link AttitudeProvider}, so the associated
 * service {@link AttitudeProvider#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)} is available.
 * ISIS Sun pointing law corresponds to an ordered attitude matching with the Sun axis
 * (X_sun, Y_sun, Z_sun) computed in GCRF frame by specific formulae.
 *
 * @concurrency not thread-safe
 * @author rodriguest
 *
 * @version $Id: IsisSunPointing.java 17582 2017-05-10 12:58:16Z bignon $
 *
 * @since 3.3
 *
 */

public class IsisSunPointing extends AbstractAttitudeLaw {

     /** Serializable UID. */
    private static final long serialVersionUID = -396768615500080537L;

    /** J axis. */
    private static final PVCoordinates PLUS_J =
        new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO);

    /** K axis. */
    private static final PVCoordinates PLUS_K =
        new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO);

    /** Sun direction in GCRF frame. */
    private final IDirection sunDirection;

    /**
     * Build a new instance of the class.
     *
     * @param sunDir
     *        the Sun direction in the inertial frame
     *
     * @throws PatriusException
     *         if the Sun cannot be built
     */
    public IsisSunPointing(final IDirection sunDir) throws PatriusException {
        super();
        this.sunDirection = sunDir;
    }

    /**
     * Build a new instance of the class.
     *
     * @param sunBody
     *        the Sun body.
     */
    public IsisSunPointing(final CelestialBody sunBody) {
        super();
        this.sunDirection = new GenericTargetDirection(sunBody);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {

        // ISIS inertial frame : GCRF
        final Frame gcrf = FramesFactory.getGCRF();

        // Satellite position/velocity
        final PVCoordinates pvSat = pvProv.getPVCoordinates(date, gcrf);

        // Normal to the orbit
        final Vector3D nOrb = pvSat.getMomentum().negate().normalize();
        final PVCoordinates nOrbPv = new PVCoordinates(nOrb, Vector3D.ZERO);

        // Sun-Satellite unitary vector
        final PVCoordinates sunSatVect = new PVCoordinates(this.sunDirection.getVector(pvProv, date, gcrf).negate(),
            Vector3D.ZERO);
        final PVCoordinates uSun = sunSatVect.normalize();

        // Compute Sun axis in inertial frame
        final PVCoordinates xSunVect = PVCoordinates.crossProduct(nOrbPv, uSun);

        // ySun is computed by zSun ^ xSun : throw orekit exception if xSun is null,
        // meaning the Sun is orthogonal to the orbit plane so ySun could not be computed
        if (xSunVect.getPosition().isZero()) {
            throw new PatriusException(PatriusMessages.ISIS_SUN_FRAME_UNDEFINED);
        }
        PVCoordinates xSun = xSunVect.normalize();

        final PVCoordinates zSun = uSun;

        // Test a condition on xSun
        xSun = (xSun.getPosition().getZ() > 0) ? xSun.negate() : xSun;
        final PVCoordinates ySun = PVCoordinates.crossProduct(zSun, xSun);
        // Compute the wanted attitude : align satellite axis with (xSun, ySun, zSun)
        final TimeStampedAngularCoordinates ac = new TimeStampedAngularCoordinates(date, ySun, zSun, PLUS_J, PLUS_K,
            1.0e-9, this.getSpinDerivativesComputation());

        // Return the attitude in the input frame
        return new Attitude(gcrf, ac).withReferenceFrame(frame, this.getSpinDerivativesComputation());
    }
}
