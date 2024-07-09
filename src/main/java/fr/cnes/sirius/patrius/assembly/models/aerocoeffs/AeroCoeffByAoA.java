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
 * @history creation 23/05/2018
 *
 * HISTORY
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models.aerocoeffs;

import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.ReentryParameters;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Aerodynamic coefficient function of the spacecraft angle of attack. Aerodynamic coefficient is
 * then retrieved by linear interpolation of an <angle of attack, aerodynamic coefficient> array.
 * 
 * @author Marie Capot
 * 
 * @version $Id$
 * 
 * @since 4.1
 * 
 */
public class AeroCoeffByAoA extends AbstractAeroCoeff1D {

    /** Serial UID. */
    private static final long serialVersionUID = -4308034491799514262L;

    /** Earth shape. */
    private final EllipsoidBodyShape earthShape;

    /**
     * Constructor.
     * 
     * @param xVariables array of x variables (angle of attack (rad))
     * @param yVariables array of y variables (aerodynamic coefficient)
     * @param earthShapeIn Earth shape
     */
    public AeroCoeffByAoA(final double[] xVariables, final double[] yVariables,
        final EllipsoidBodyShape earthShapeIn) {
        super(xVariables, yVariables);
        this.earthShape = earthShapeIn;
    }

    /** {@inheritDoc} */
    @Override
    protected double computeXVariable(final SpacecraftState state) throws PatriusException {
        return angleOfAttackFromSpacecraftState(state, this.earthShape);
    }

    /**
     * Computes the angle of attack from the spacecraft state and the Earth shape.
     * 
     * @param state the spacecraft state
     * @param earthShape the Earth shape
     * @return the mach number
     * @throws PatriusException if the transformation from state frame to body frame cannot be
     *         computed
     */
    public static double angleOfAttackFromSpacecraftState(final SpacecraftState state,
                                                          final EllipsoidBodyShape earthShape) throws PatriusException {

        // Geodetic point and topocentric frame
        final GeodeticPoint geodeticPoint = earthShape.transform(state.getPVCoordinates()
            .getPosition(), state.getFrame(), state.getDate());
        final TopocentricFrame topocentricFrame = new TopocentricFrame(earthShape, geodeticPoint,
            0, "TopoPATRIUS");

        // JPSIMU topocentric frame.
        final Transform topoPatriusToTopoPSIMU = new Transform(state.getDate(), new Rotation(
            RotationOrder.XYZ, FastMath.PI, 0, 0));
        // Topocentric frame
        final Frame topocentricFramePsimu = new Frame(topocentricFrame, topoPatriusToTopoPSIMU,
            "TopoPSimu");

        // Azimuth and elevation of the velocity in body frame
        final CartesianParameters carPar = new CartesianParameters(
            state.getPVCoordinates(earthShape.getBodyFrame()), state.getMu());
        final ReentryParameters reentryPar = carPar.getReentryParameters(
            earthShape.getEquatorialRadius(), earthShape.getFlattening());

        // Velocity direction expressed in Aircraft-carried normal earth axis system frame
        final double slope = reentryPar.getSlope();
        final double azimut = reentryPar.getAzimuth();
        final double[] sincosSlope = MathLib.sinAndCos(slope);
        final double sinSlope = sincosSlope[0];
        final double cosSlope = sincosSlope[1];
        final double[] sincosAzimut = MathLib.sinAndCos(azimut);
        final double sinAzimut = sincosAzimut[0];
        final double cosAzimut = sincosAzimut[1];
        final Vector3D velEarthVec = new Vector3D(cosSlope * cosAzimut,
                cosSlope * sinAzimut, -sinSlope);

        // Yaw, pitch, roll
        final double[] yawPitchRoll = state.getAttitude().withReferenceFrame(topocentricFramePsimu)
            .getRotation().getAngles(RotationOrder.ZYX);

        // Rotation Aircraft-carried normal earth axis system frame -> Aircraft frame
        final Rotation earthToAircraft = new Rotation(RotationOrder.ZYX, yawPitchRoll[0],
            yawPitchRoll[1], yawPitchRoll[2]);

        // Velocity direction expressed in Aircraft frame
        final Vector3D velAircraftVec = earthToAircraft.applyInverseTo(velEarthVec);

        // Angle of Attack
        return MathLib.atan2(velAircraftVec.getZ(), velAircraftVec.getX());
    }

    /** {@inheritDoc} */
    @Override
    public AerodynamicCoefficientType getType() {
        return AerodynamicCoefficientType.AOA;
    }
}
