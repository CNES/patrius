/**
 * HISTORY
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3150:10/05/2022:[PATRIUS] Absence d'attitude lors de l'utilisation du mode Ephemeris du propagateur 
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.propagation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKutta6Integrator;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class created for FT-3150 needed to validate the correct set of the attitude for a numerical propagator in
 * ephemeris mode.
 * 
 * @author anardi
 *
 */
public class PropagationTest {

    /**
     * Test for FT-3150 concerning the correct set of the attitude for a numerical propagator in ephemeris mode.
     * 
     * @throws PatriusException if an error occurs while creating the attitude provider, propagating, getting or
     *         shifting the spacecraft state or getting the spacecraft attitude
     */
    @Test
    public void attitudeTest() throws PatriusException {

        // Set data root
        Utils.setDataRoot("regular-dataPBASE");



        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH.shiftedBy(5.0);
        final Orbit initialOrbit = new KeplerianOrbit(7000e3, 0.1, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.EIGEN5C_EARTH_MU);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        // Propagator
        final FirstOrderIntegrator integ = new RungeKutta6Integrator(5);
        final NumericalPropagator prop = new NumericalPropagator(integ, initialState.getFrame(), OrbitType.CARTESIAN,
            PositionAngle.TRUE);

        prop.setInitialState(initialState);
        prop.setEphemerisMode();

        // End date
        final AbsoluteDate endDate = initialDate.shiftedBy(86400 * 1);

        // Attitude
        final AttitudeProvider attProv = new LofOffset(FramesFactory.getGCRF(), LOFType.QSW);

        // Propagation
        prop.propagate(endDate);
        // Set the attitude provider by default after the propagation
        prop.setAttitudeProvider(attProv);
        // Retrieve the ephemerides after having set the attitude provider after the propagation
        final BoundedPropagator ephem = prop.getGeneratedEphemeris();

        // Check that the attitude provider by default is the one set
        final AttitudeProvider attProvByDefault = ephem.getAttitudeProvider();
        Assert.assertEquals(attProv, attProvByDefault);

        // Check that the attitude forces provider is null
        final AttitudeProvider attProvForces = ephem.getAttitudeProviderForces();
        Assert.assertEquals(null, attProvForces);

        // Check that the attitude events provider is null
        final AttitudeProvider attProvEvents = ephem.getAttitudeProviderEvents();
        Assert.assertEquals(null, attProvEvents);

        final SpacecraftState sc = ephem.getSpacecraftState(initialDate).shiftedBy(86400.0 / 2);
        Assert.assertNotNull(sc.getAttitude());

    }

}
