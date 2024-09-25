/**
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2299:27/05/2020:Implementation du propagateur analytique de Liu 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 *
 **/
/**
 * Test class for {@link LiuMeanOsculatingConverter}.
 * @author Noe Charpigny
 */

public class LiuMeanOsculatingConverterTest {
	
	/** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Liu propagator
         * @featureDescription Validate the Liu propagator
         */
        LIU_PROPAGATOR
    }
    
    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#LIU_PROPAGATOR}
     * @testedMethod {@link LiuMeanOsculatingConverter#mean2osc(Orbit)}
     * @description Test whether expressing the orbit in another frame has an influence on
     * the propagated orbital parameters. Test whether the orbit nature ( Keplerian, Cartesian )
     * has an influence as well.
     * @testPassCriteria Propagated orbital parameter values does not change (tolerance: 1E-12)
     */
    @Test
    public void testFrames() throws PatriusException {
    	
    	Utils.setDataRoot("regular-dataPBASE");

    	// Declaring the tolerance.
    	final double eps = 1E-12;

        Report.printMethodHeader("testFrames", "Mean <=> Osculating conversion with different frames", "Math", eps,
            ComparisonType.RELATIVE);
    	
        // Declaring the date and the frames.
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(19532 * 86400.);
        final Frame frame1 = FramesFactory.getCIRF();
        final Frame frame2 = FramesFactory.getGCRF();
        
        // Declaring Keplerian orbit 1 input parameters.
        final double a = 7000000 ;
        final double e = 0.01;
        final double i = FastMath.toRadians(89.98);
        final double pa = FastMath.PI / 2.;
        final double raan = 0.1;
        final double anomaly = 0.2; 
        final PositionAngle type = PositionAngle.MEAN; 
        final double mu = 3.98600442e+14;
        
        // Building the first orbit expressed in the first frame.
        final Orbit orbit1 = new KeplerianOrbit(a, e, i, pa, raan, anomaly, type, frame1, date, mu);
        
        // Building the second orbit, which is the first orbit expressed in the second frame.
        final Orbit orbit2 = new CartesianOrbit(orbit1.getPVCoordinates(frame2), frame2, date, mu);
        
        // Declaring propagator input parameters.
        final double referenceRadius = 6378136.3 ;
        final double j2 = 0.001082626613;
        final Frame propagatorFrame = FramesFactory.getCIRF();
        
        // Building the propagator.
        final LiuMeanOsculatingConverter propagator = new LiuMeanOsculatingConverter(referenceRadius, mu, j2, propagatorFrame);
        
        // Computing the osculating parameters from the first and from the second orbit.
        final KeplerianOrbit osc1 = (KeplerianOrbit) propagator.mean2osc(orbit1);
        final Orbit osc2 = propagator.mean2osc(orbit2);
        final KeplerianOrbit osc2InCIRF = new KeplerianOrbit(osc2.getPVCoordinates(frame1), frame1, date, mu);

        // Checking that the two sets of osculating parameters are equal.
        // Check that the parameters of the orbit converted twice is equal to the parameters of the initial orbit.
        Report.printToReport("a", osc1.getA(), osc2InCIRF.getA());
        Report.printToReport("e", osc1.getE(), osc2InCIRF.getE());
        Report.printToReport("i", osc1.getI(), osc2InCIRF.getI());
        Report.printToReport("Pa", osc1.getPerigeeArgument(), osc2InCIRF.getPerigeeArgument());
        Report.printToReport("RAAN", osc1.getRightAscensionOfAscendingNode(), osc2InCIRF.getRightAscensionOfAscendingNode());
        Report.printToReport("M", osc1.getMeanAnomaly(), osc2InCIRF.getMeanAnomaly());

        Assert.assertEquals(FramesFactory.getGCRF(), osc2.getFrame());
        Assert.assertEquals(OrbitType.CARTESIAN, osc2.getType());
        Assert.assertEquals(0., relDiff(osc1.getA(), osc2InCIRF.getA()), eps);
        Assert.assertEquals(0., relDiff(osc1.getE(), osc2InCIRF.getE()), eps);
        Assert.assertEquals(0., relDiff(osc1.getI(), osc2InCIRF.getI()), eps);
        Assert.assertEquals(0., relDiff(osc1.getPerigeeArgument(), osc2InCIRF.getPerigeeArgument()), eps);
        Assert.assertEquals(0.,
            relDiff(osc1.getRightAscensionOfAscendingNode(), osc2InCIRF.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0.,
            relDiff(osc1.getMeanAnomaly() % (2. * FastMath.PI), osc2InCIRF.getMeanAnomaly() % (2. * FastMath.PI)), eps);
    }
  
    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#LIU_PROPAGATOR}
     * @testedMethod {@link LiuMeanOsculatingConverter#mean2osc(Orbit)}
     * @testedMethod {@link LiuMeanOsculatingConverter#osc2mean(Orbit)}
     * @description test to convert twice an orbit with Liu propagator 
     * methods and check that it is equal to the initial orbit
     * @testPassCriteria orbit is as expected (reference : identity, tolerance: 5E-15)
     */
    @Test
    public void testConversionAR() throws PatriusException {
        
    	final double eps = 1E-14;

        // Initialization
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final KeplerianOrbit orbit = new KeplerianOrbit(7000000, 0.1, 0.001, FastMath.PI / 2., 0.1, 0.2, PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        
        final double referenceRadius = 6378136.3 ;
        final double mu = 3.98600442e+14 ;
        final double j2 = -0.001082626613;
        final Frame frame = FramesFactory.getCIRF();
        
        final LiuMeanOsculatingConverter propagator = new LiuMeanOsculatingConverter(referenceRadius, mu, j2, frame);
 
        // Mean to osculating conversion
        final KeplerianOrbit osc = (KeplerianOrbit) propagator.mean2osc(orbit);
        
        // Osculating to mean conversion
        final KeplerianOrbit mean = (KeplerianOrbit) propagator.osc2mean(osc);
        
        // Check that the parameters of the orbit converted twice is equal to the parameters of the initial orbit.
        Assert.assertEquals(0., relDiff(orbit.getA(), mean.getA()), eps);
        Assert.assertEquals(0., relDiff(orbit.getE(), mean.getE()), eps);
        Assert.assertEquals(0., relDiff(orbit.getI(), mean.getI()), eps);
        Assert.assertEquals(0., relDiff(orbit.getPerigeeArgument(), mean.getPerigeeArgument()), eps);
        Assert.assertEquals(0.,
            relDiff(orbit.getRightAscensionOfAscendingNode(), mean.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0.,
            relDiff(orbit.getMeanAnomaly() % (2. * FastMath.PI), mean.getMeanAnomaly() % (2. * FastMath.PI)), eps);
    }
    
    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#LIU_PROPAGATOR}
     * @testedMethod {@link LiuMeanOsculatingConverter#setThreshold(double)}
     * @testedMethod {@link LiuMeanOsculatingConverter#osc2mean(Orbit)}
     * @description Test that the setThreshold(double) method works.
     * Test that osc2mean(orbit) method throws a PatriusException when called, if convergence threshold os too low..
     * @testPassCriteria Exception is thrown and is the "UNABLE_TO_COMPUTE_LIU_MEAN_PARAMETERS" Patrius message.
     */
    @Test
    public void testThreshold() throws PatriusException {
           
        // Declaring Keplerian orbit 1 input parameters.
        final double a = 7000000 ;
        final double e = 0.01;
        final double i = FastMath.toRadians(89.98);
        final double pa = FastMath.PI / 2.;
        final double raan = 0.1;
        final double anomaly = 0.2; 
        final PositionAngle type = PositionAngle.MEAN; 
        final double mu = 3.98600442e+14;
        final Frame frame = FramesFactory.getCIRF();
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(19532 * 86400.);
        
        // Building the first orbit expressed in the first frame.
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, anomaly, type, frame, date, mu);
        
        // Declaring propagator input parameters. 
        final double referenceRadius = 6378136.3 ;
        final double j2 = -0.001082626613;
        final Frame propagatorFrame = FramesFactory.getCIRF();
        
        // Building the propagator.
        final LiuMeanOsculatingConverter propagator = new LiuMeanOsculatingConverter(referenceRadius, mu, j2, propagatorFrame);
 
        // Reducing threshold value to raise a Patrius Exception.
        final double eps = 1E-18;
        propagator.setThreshold(eps);
                
        // Calling the osc2mean(orbit) method.
    	// It is supposed to throw a PatriusException, as the convergence threshold eps is way too low.
        try { 
        	// Osculating to mean conversion
            propagator.osc2mean(orbit);
            Assert.fail();
        } catch (final PatriusException exception) {
        	// Assert whether the exception is thrown
        	Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#LIU_PROPAGATOR}
     * @testedMethod {@link LiuMeanOsculatingConverter#mean2osc(Orbit)}
     * @testedMethod {@link LiuMeanOsculatingConverter#osc2mean(Orbit)}
     * @description test the Liu mean <=> osculating conversion
     * @input Keplerian orbit in mean/osculating parameters
     * @output Keplerian orbit in osculating/mean parameters
     * @testPassCriteria orbit is as expected (reference : Lyddane, tolerance: 5E-15)
     */
    @Test
    public void testMeanOscConversion() throws PatriusException {

        final double eps1 = 5E-2;
        final double eps2 = 2E-12;

        Report.printMethodHeader("testMeanOscConversion", "Mean <=> Osculating conversion", "Lyddane secular", eps1,
            ComparisonType.RELATIVE);

        // Declaring Keplerian orbit input parameters.
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final double a = 7000000 ;
        final double e = 0.01;
        final double i = FastMath.toRadians(89.98);
        final double pa = FastMath.PI / 2.;
        final double raan = 0.1;
        final double anomaly = 0.2; 
        final PositionAngle type = PositionAngle.MEAN; 
        final Frame frame = FramesFactory.getCIRF();
        final double mu = 3.98600442e+14;
        
        // Building orbit
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, anomaly, type, frame, date, mu);
        
        // Declaring propagators input parameters.
        final double referenceRadius = 6378136.3; 
        final double c20 = -0.001082626613;
        final double j2 = -c20;
        final Frame propagatorFrame = FramesFactory.getCIRF();
        
        // Building Liu propagator.
        final LiuMeanOsculatingConverter propagator1 = new LiuMeanOsculatingConverter(referenceRadius, mu, j2, propagatorFrame);
        		
        // Building Lyddane Secular propagator.
        final LyddaneSecularPropagator propagator2 = new LyddaneSecularPropagator(orbit,
        		referenceRadius, mu, c20, 0.0, 0.0, 0.0,frame, ParametersType.MEAN);
        
        // Converting mean parameters to osculating parameters. 
        final KeplerianOrbit osc1 = (KeplerianOrbit) propagator1.mean2osc(orbit);
        final KeplerianOrbit osc2 = (KeplerianOrbit) propagator2.mean2osc(orbit);

        // Check results
        Report.printToReport("a", osc1.getA(), osc2.getA());
        Report.printToReport("e", osc1.getE(), osc2.getE());
        Report.printToReport("i", osc1.getI(), osc2.getI());
        Report.printToReport("Pa", osc1.getPerigeeArgument(), osc2.getPerigeeArgument());
        Report.printToReport("RAAN", osc1.getRightAscensionOfAscendingNode(), osc2.getRightAscensionOfAscendingNode());
        Report.printToReport("M", osc1.getMeanAnomaly(), osc2.getMeanAnomaly());

        // Assert
        Assert.assertEquals(0., relDiff(osc1.getA(), osc2.getA()), eps2);
        Assert.assertEquals(0., relDiff(osc1.getE(), osc2.getE()), eps1);
        Assert.assertEquals(0., relDiff(osc1.getI(), osc2.getI()), eps2);
        Assert.assertEquals(0., relDiff(osc1.getPerigeeArgument(), osc2.getPerigeeArgument()), eps1);
        Assert.assertEquals(0.,
            relDiff(osc1.getRightAscensionOfAscendingNode(), osc2.getRightAscensionOfAscendingNode()), eps2);
        Assert.assertEquals(0., relDiff(osc1.getMeanAnomaly(), osc2.getMeanAnomaly()), eps1);
    }

	 /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#LIU_PROPAGATOR}
     * @testedMethod {@link LiuMeanOsculatingConverter#propagateMeanOrbit(AbsoluteDate)}
     * @description Test that the propagateMeanOrbit method throw a PatriusException when called.
     * @testPassCriteria An exception is thrown, and is the "METHOD_NOT_AVAILABLE_LIU" Patrius message.
     */
    @Test
    public void testPropagateMeanOrbit() throws PatriusException {
    	
    	// Declaring the date until which the mean orbit is supposed to be propagated.
    	final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(19532 * 86400.);
    	        
        // Declaring propagator input parameters.
        final double referenceRadius = 6378136.3 ;
        final double j2 = 0.001082626613;
        final Frame propagatorFrame = FramesFactory.getCIRF();
        final double mu = 3.98600442e+14;
        
        // Building the propagator.
        final LiuMeanOsculatingConverter propagator = new LiuMeanOsculatingConverter(referenceRadius, mu, j2, propagatorFrame);
    	
    	// Calling the propagateMeanOrbit method.
    	// It is supposed to throw a PatriusException, as this method is not implemented in LiuPropagator.
        try { 
            propagator.propagateMeanOrbit(date);
        	Assert.fail();
        } 
        catch (final PatriusException e) {
        	// Assert whether the exception is thrown. 
            Assert.assertTrue(true);
        }
    }
    
    /**
     * Compute relative difference.
     * 
     * @param expected
     *        expected
     * @param actual
     *        actual
     * @return relative difference
     */
    private static double relDiff(final double expected, final double actual) {
        if (expected == 0) {
            return MathLib.abs(expected - actual);
        }
        return MathLib.abs((expected - actual) / expected);
    }
}
